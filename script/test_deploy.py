import json
import os
from pathlib import Path
import shutil
import stat
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


def load_yaml(path):
    return json.loads(subprocess.check_output([
        'ruby', '-ryaml', '-rjson', '-e',
        'puts JSON.generate(YAML.load_file(ARGV[0]))', str(path)
    ]))


WORKFLOW = load_yaml(ROOT / '.github/workflows/ci.yml')
STEPS = {step['name']: step for step in WORKFLOW['jobs']['deploy']['steps']}
RENDER = STEPS['Render runtime .env']['run']
RESOLVE = STEPS['Resolve deploy target']['run']
REMOTE = STEPS['Deploy and verify over SSH']['run'].split("<<'REMOTE'\n", 1)[1].split('\nREMOTE', 1)[0]
REDIS_IMAGE = load_yaml(ROOT / 'docker-compose.yml')['services']['redis']['image']
VALID = (
    "MYSQL_HOST=host.docker.internal\nMASTER_DATASOURCE_USERNAME=erp_test\n"
    "MASTER_DATASOURCE_PASSWORD='canary$p #word'\nREDIS_HOST=redis\n"
    "REDIS_PASSWORD='canary$r # \"quoted\"'\n"
    "MYBATIS_ENCRYPTOR_PASSWORD='0123456789abcdef'\n"
)


class DeployChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='deploy-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for name in ('.env.example', 'docker-compose.yml'):
            shutil.copy2(ROOT / name, self.root / name)
        self.project = self.root.name.replace('_', '-')
        self.env = {key: value for key, value in os.environ.items()
                    if key in ('PATH', 'HOME', 'DOCKER_HOST', 'DOCKER_CONTEXT', 'DOCKER_CONFIG',
                               'DOCKER_TLS_VERIFY', 'DOCKER_CERT_PATH', 'TMPDIR')}
        self.env.update(APP_ENV=VALID, PROJECT=self.project, TAG='a' * 40,
                        HOST_PORT='8022', NAME=self.project, APP_ENV_FILE='.env.deploy')

    def render(self, content=VALID):
        return subprocess.run(['bash', '-e', '-c', RENDER], cwd=self.root,
                              env={**self.env, 'APP_ENV': content}, capture_output=True, text=True)

    def compose(self, *args, project=None, check=True):
        result = subprocess.run(['docker', 'compose', '-p', project or self.project,
                                 '--env-file', '.env.deploy', *args], cwd=self.root,
                                env=self.env, capture_output=True, text=True, timeout=90)
        if check:
            self.assertEqual(result.returncode, 0, result.stderr)
        return result

    def config(self):
        return json.loads(self.compose('config', '--format', 'json').stdout)

    def test_valid_literals_and_service_boundaries(self):
        result = self.render()
        self.assertEqual(result.returncode, 0, result.stderr)
        visible = '\n'.join(line for line in result.stdout.splitlines() if not line.startswith('::add-mask::'))
        self.assertNotIn('canary', visible + result.stderr)
        self.assertIn('::add-mask::canary$p #word', result.stdout)
        self.assertEqual(stat.S_IMODE((self.root / '.env.deploy').stat().st_mode), 0o600)
        services = self.config()['services']
        app, redis = services['app'], services['redis']
        self.assertEqual(app['environment']['MASTER_DATASOURCE_PASSWORD'], 'canary$$p #word')
        self.assertEqual(app['environment']['SPRING_PROFILES_ACTIVE'], 'prod')
        self.assertEqual(app['ports'][0]['host_ip'], '127.0.0.1')
        self.assertEqual(app['ports'][0]['target'], 48080)
        self.assertEqual(app['depends_on']['redis']['condition'], 'service_healthy')
        self.assertEqual(set(redis['environment']), {'REDIS_PASSWORD', 'REDIS_PORT'})
        self.assertEqual(redis['environment']['REDIS_PASSWORD'], app['environment']['REDIS_PASSWORD'])
        self.assertFalse(redis.get('ports'))
        self.assertNotIn('container_name', redis)
        self.assertIn('--appendonly yes', redis['command'][2])
        self.assertNotIn('canary', redis['command'][2])
        self.assertEqual(self.compose('config', '--images', 'redis').stdout.strip(), REDIS_IMAGE)

    def test_shell_overrides_keep_redis_and_app_aligned(self):
        self.env.update(REDIS_PASSWORD='canary$ambient # value', REDIS_PORT='6382')
        self.assertEqual(self.render().returncode, 0)
        services = self.config()['services']
        for key in ('REDIS_PASSWORD', 'REDIS_PORT'):
            self.assertEqual(services['app']['environment'][key], services['redis']['environment'][key])
        self.assertEqual(services['redis']['environment']['REDIS_PORT'], '6382')
        self.assertEqual(services['redis']['environment']['REDIS_PASSWORD'], 'canary$$ambient # value')

    def test_real_container_receives_literal_dollar(self):
        self.assertEqual(self.render().returncode, 0)
        config = {'services': {'app': {'image': REDIS_IMAGE, 'network_mode': 'none',
                  'env_file': '.env.deploy', 'entrypoint': ['sh', '-c'],
                  'command': ['printf \'%s\' "$$MASTER_DATASOURCE_PASSWORD"']}}}
        (self.root / 'literal.json').write_text(json.dumps(config))
        result = self.compose('-f', 'literal.json', 'run', '--rm', '--no-deps', '--pull', 'never', '-T', 'app')
        self.assertEqual(result.stdout, 'canary$p #word')

    def test_template_after_filling_required_values(self):
        text = (self.root / '.env.example').read_text()
        for key, value in {'MASTER_DATASOURCE_USERNAME': 'erp_test', 'MASTER_DATASOURCE_PASSWORD': "'canary$pw'",
                           'REDIS_PASSWORD': "'canary$redis'", 'MYBATIS_ENCRYPTOR_PASSWORD': "'0123456789abcdef'"}.items():
            text = text.replace(f'\n{key}=\n', f'\n{key}={value}\n')
        result = self.render(text)
        self.assertEqual(result.returncode, 0, result.stderr)
        environment = self.config()['services']['app']['environment']
        self.assertNotIn('XQ_RPA_CALLBACK_BASE_URL', environment)
        self.assertNotIn('ANTHROPIC_BASE_URL', environment)

    def test_staged_and_installed_compose_resolve_identically(self):
        self.assertEqual(self.render().returncode, 0)
        with (self.root / '.env.deploy').open('a') as file:
            file.write('HOST_BIND_IP=172.17.0.1\n')
        stage = self.root / '.deploy.candidate'
        stage.mkdir(mode=0o700)
        for name in ('.env.deploy', 'docker-compose.yml'):
            shutil.move(self.root / name, stage / name)
        candidate = subprocess.run(['docker', 'compose', '--project-directory', str(self.root),
                                    '--env-file', str(stage / '.env.deploy'), '-f', str(stage / 'docker-compose.yml'),
                                    'config', '--format', 'json'], capture_output=True, text=True,
                                   env={**self.env, 'APP_ENV_FILE': str(stage / '.env.deploy')})
        self.assertEqual(candidate.returncode, 0, candidate.stderr)
        shutil.move(stage / '.env.deploy', self.root / '.env')
        shutil.move(stage / 'docker-compose.yml', self.root / 'docker-compose.yml')
        installed = subprocess.run(['docker', 'compose', 'config', '--format', 'json'], cwd=self.root,
                                   env={**self.env, 'APP_ENV_FILE': '.env'}, capture_output=True, text=True)
        self.assertEqual(installed.returncode, 0, installed.stderr)
        self.assertEqual(json.loads(candidate.stdout), json.loads(installed.stdout))

    def test_port_override_and_complete_jdbc_url(self):
        content = VALID.replace('MYSQL_HOST=host.docker.internal', "MASTER_DATASOURCE_URL='jdbc:mysql://database:3306/app?a=b&c=d'")
        self.assertEqual(self.render(content + 'SERVER_PORT=9999\nREDIS_PORT=6381\n').returncode, 0)
        services = self.config()['services']
        self.assertEqual(services['app']['ports'][0]['target'], 9999)
        self.assertEqual(services['app']['environment']['REDIS_PORT'], '6381')
        self.assertEqual(services['redis']['environment']['REDIS_PORT'], '6381')

    def test_rejected_inputs_never_echo_raw_values(self):
        cases = {
            'unset': '',
            'bare-secret': VALID + 'canaryMalformedSecret\n',
            'yaml-colon': VALID + 'OPENAI_API_KEY: canarySecret\n',
            'duplicate': VALID + 'REDIS_HOST=canaryDuplicate\n',
            'topology': VALID + 'HOST_BIND_IP=0.0.0.0\n',
            'unknown': VALID + 'MYSL_HOST=canaryTypo\n',
            'empty-password': VALID.replace("'canary$p #word'", ''),
            'quoted-empty-password': VALID.replace("'canary$p #word'", "''"),
            'whitespace-password': VALID.replace("'canary$p #word'", '   '),
            'no-user': VALID.replace('MASTER_DATASOURCE_USERNAME=erp_test\n', ''),
            'no-redis-host': VALID.replace('REDIS_HOST=redis\n', ''),
            'external-redis': VALID.replace('REDIS_HOST=redis', 'REDIS_HOST=host.docker.internal'),
            'no-redis-password': '\n'.join(line for line in VALID.splitlines() if not line.startswith('REDIS_PASSWORD=')),
            'empty-redis-password': VALID.replace("'canary$r # \"quoted\"'", "''"),
            'bad-redis-port': VALID + 'REDIS_PORT=65536\n',
            'bad-redis-db': VALID + 'REDIS_DATABASE=16\n',
            'no-encryptor': VALID.replace("MYBATIS_ENCRYPTOR_PASSWORD='0123456789abcdef'\n", ''),
            'bad-key-size': VALID.replace('0123456789abcdef', 'canaryInvalidSize'),
            'empty-port': VALID + 'SERVER_PORT=\n',
            'bad-port': VALID + 'SERVER_PORT=65536\n',
            'unquoted-dollar': VALID.replace("'canary$p #word'", 'canary$WORD'),
            'doublequoted-dollar': VALID.replace("'canary$p #word'", '"canary$WORD"'),
            'bad-quote': VALID + "OPENAI_API_KEY='canaryUnclosed\n",
        }
        for name, text in cases.items():
            with self.subTest(name=name):
                result = self.render(text)
                self.assertNotEqual(result.returncode, 0)
                self.assertNotIn('canary', result.stdout + result.stderr)
                self.assertFalse((self.root / '.env.deploy').exists())

    def test_resolve_targets_and_reject_shell_input(self):
        for branch, port in [('main', '8020'), ('test', '8021'), ('dev', '8022')]:
            output = self.root / ('target-' + branch)
            env = {**self.env, 'HOST': 'deploy.example.internal', 'SSH_USER': 'deploy', 'BASE_PATH': '/srv/backend/',
                   'GITHUB_REF_NAME': branch, 'GITHUB_OUTPUT': str(output)}
            result = subprocess.run(['bash', '-c', RESOLVE], env=env, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0)
            self.assertIn('port=' + port, output.read_text())
            self.assertIn('/srv/backend/' + branch, output.read_text())
        for key, value in [('HOST', 'host;uname'), ('SSH_USER', '-oProxyCommand=uname'),
                           ('BASE_PATH', "/srv/'bad"), ('BASE_PATH', '/srv/../etc'), ('BASE_PATH', '')]:
            with self.subTest(key=key):
                result = subprocess.run(['bash', '-c', RESOLVE], env={**env, key: value}, capture_output=True, text=True)
                self.assertNotEqual(result.returncode, 0)

    def test_redis_auth_persistence_isolation_and_custom_port(self):
        self.assertEqual(self.render(VALID + 'REDIS_PORT=6381\n').returncode, 0)
        second = self.project + '-second'
        for project in (self.project, second):
            self.addCleanup(lambda project=project: self.compose('down', '-v', project=project))
            self.compose('up', '-d', '--pull', 'never', '--wait', '--wait-timeout', '60', 'redis', project=project)
        unauthenticated = self.compose('exec', '-T', 'redis', 'redis-cli', '-p', '6381', 'ping')
        self.assertIn('NOAUTH', unauthenticated.stdout)
        wrong = self.compose('exec', '-T', 'redis', 'sh', '-c',
                             'REDISCLI_AUTH=wrong redis-cli -p "$REDIS_PORT" ping', check=False)
        self.assertNotIn('PONG', wrong.stdout)
        cli = 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli --raw -p "$REDIS_PORT" '
        self.assertEqual(self.compose('exec', '-T', 'redis', 'sh', '-c', cli + 'ping').stdout.strip(), 'PONG')
        network = subprocess.run(['docker', 'run', '--rm', '--pull', 'never', '--network', self.project + '_default',
                                  '-e', 'REDISCLI_AUTH', REDIS_IMAGE, 'redis-cli', '-h', 'redis', '-p', '6381', 'ping'],
                                 env={**self.env, 'REDISCLI_AUTH': 'canary$r # "quoted"'},
                                 capture_output=True, text=True, timeout=30)
        self.assertEqual(network.returncode, 0, network.stderr)
        self.assertEqual(network.stdout.strip(), 'PONG')
        self.compose('exec', '-T', 'redis', 'sh', '-c', cli + 'set deployment-test stored')
        self.assertEqual(self.compose('exec', '-T', 'redis', 'sh', '-c', cli + 'get deployment-test',
                                      project=second).stdout.strip(), '')
        cid = self.compose('ps', '-q', 'redis').stdout.strip()
        top = subprocess.check_output(['docker', 'top', cid, '-eo', 'pid,user,args'], text=True)
        self.assertNotIn('canary', top)
        self.assertIn('redis-server', top)
        self.compose('up', '-d', '--force-recreate', '--pull', 'never', '--wait', '--wait-timeout', '60', 'redis')
        self.assertEqual(self.compose('exec', '-T', 'redis', 'sh', '-c', cli + 'get deployment-test').stdout.strip(), 'stored')
        self.assertEqual(self.compose('exec', '-T', 'redis', 'stat', '-c', '%a', '/run/redis/users.acl').stdout.strip(), '600')

    def remote_fixture(self, mode):
        root = self.root / mode
        root.mkdir()
        bin_dir = root / 'bin'
        bin_dir.mkdir()
        dest = root / 'deploy'
        dest.mkdir()
        stage = dest / '.deploy.test'
        stage.mkdir(mode=0o700)
        (dest / '.env').write_text('old-config')
        (dest / 'docker-compose.yml').write_text('old-compose')
        (stage / '.env.deploy').write_text('new-config')
        (stage / '.env.deploy').chmod(0o600)
        (stage / 'docker-compose.yml').write_text('new-compose')
        fake = '''import json, os, pathlib, sys
args=sys.argv[1:]
mode=os.environ['MODE']
with open(os.environ['CALLS'], 'a') as f: f.write(json.dumps(args)+'\\n')
name=pathlib.Path(sys.argv[0]).name
if name=='flock': sys.exit(0)
if name=='curl':
    pathlib.Path(args[args.index('-o')+1]).write_text(json.dumps({'status': 'DOWN' if mode=='down' else 'UP'}))
    print('302' if mode=='redirect' else '200', end=''); sys.exit(0)
if args[:2]==['image','inspect']:
    print('sha256:redis' if args[2]=='redis:test' else 'sha256:app'); sys.exit(0)
if args[0]=='inspect':
    redis=args[-1]=='redis-id'
    if args[2]=='{{.Image}}':
        print('sha256:wrong' if mode==('wrong-redis' if redis else 'wrong-image') else ('sha256:redis' if redis else 'sha256:app'))
    else: print('unhealthy' if redis and mode=='redis-unhealthy' else 'healthy')
    sys.exit(0)
for command in ('config','pull','up','ps'):
    if command in args:
        if mode==command+'-fail': print('canarySecretError',file=sys.stderr);sys.exit(1)
        if command=='ps': print('redis-id' if args[-1]=='redis' else 'app-id')
        if command=='config' and '--images' in args: print('redis:test')
        sys.exit(0)
sys.exit(1)
'''
        for name in ('docker', 'curl', 'flock'):
            executable = bin_dir / name
            executable.write_text('#!' + sys.executable + '\n' + fake)
            executable.chmod(0o700)
        calls = root / 'calls'
        result = subprocess.run(['bash', '-c', REMOTE, 'test', str(dest), str(stage), 'a'*40, '8022', '172.17.0.1'],
                                env={**self.env, 'PATH': str(bin_dir) + ':' + os.environ['PATH'], 'MODE': mode, 'CALLS': str(calls)},
                                capture_output=True, text=True)
        self.assertNotIn('canary', result.stdout + result.stderr)
        self.assertFalse(stage.exists())
        self.assertNotIn('prune', calls.read_text())
        if mode == 'success':
            commands = [json.loads(line) for line in calls.read_text().splitlines()]
            self.assertTrue(any('pull' in call and call[-2:] == ['app', 'redis'] for call in commands))
            self.assertTrue(any('up' in call and '--pull' in call and call[-2:] == ['app', 'redis'] for call in commands))
        return result, dest

    def test_remote_success_and_failed_deploy_do_not_publish_bad_config(self):
        result, dest = self.remote_fixture('success')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((dest / '.env').read_text(), 'new-config')
        self.assertEqual(stat.S_IMODE((dest / '.env').stat().st_mode), 0o600)
        for mode in ('config-fail', 'pull-fail', 'up-fail', 'wrong-image', 'wrong-redis', 'redis-unhealthy', 'redirect', 'down'):
            with self.subTest(mode=mode):
                result, dest = self.remote_fixture(mode)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual((dest / '.env').read_text(), 'old-config')
                self.assertEqual((dest / 'docker-compose.yml').read_text(), 'old-compose')


if __name__ == '__main__':
    if subprocess.run(['docker', 'image', 'inspect', REDIS_IMAGE], stdout=subprocess.DEVNULL,
                      stderr=subprocess.DEVNULL).returncode:
        subprocess.run(['docker', 'pull', REDIS_IMAGE], check=True)
    unittest.main(verbosity=2)
