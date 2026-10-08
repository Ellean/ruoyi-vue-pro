import zipfile,re
j=r'C:\Users\admin\.m2\repository\org\springframework\ai\spring-ai-openai\2.0.0\spring-ai-openai-2.0.0.jar'
z=zipfile.ZipFile(j)
for name in sorted(z.namelist()):
    if 'HttpClient' in name or 'HttpClientBuilderCustomizer' in name:
        if name.endswith('.class'):
            print(name)
print('--- OpenAiChatModel Builder methods ---')
data=z.read('org/springframework/ai/openai/OpenAiChatModel$Builder.class')
ss=[t.decode() for t in re.findall(rb'[\x20-\x7e]{4,}', data)]
for s in ss:
    if any(k in s.lower() for k in ['http','custom','protocol','timeout','builder','okhttp']):
        print(s)
print('--- SpringAiOpenAiHttpClient Builder ---')
data=z.read('org/springframework/ai/openai/http/okhttp/SpringAiOpenAiHttpClient$Builder.class')
ss=[t.decode() for t in re.findall(rb'[\x20-\x7e]{4,}', data)]
for s in ss:
    if any(k in s.lower() for k in ['protocol','okhttp','timeout','custom','build','proxy']):
        print(s)
