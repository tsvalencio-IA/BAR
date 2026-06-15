# Controle BAR v1.5.0 — UX 10/10, gestor protegido e tema claro/escuro

Projeto Android nativo para GitHub Actions gerar APK.

## Destaques da v1.5.0

- Base preservada da linha funcional v1.4.x.
- Gestor protegido por PIN administrativo.
- Atendente não consegue transformar o aparelho em GESTOR sem o PIN.
- PIN inicial do gestor: `*177`.
- Opção para alterar o PIN do gestor em Configurações avançadas.
- Tema escuro e tema claro dentro do app.
- Visual refinado em estilo Controle BAR: preto/dourado no tema escuro e versão clara para uso diurno.
- Menus separados para GESTOR e ATENDENTE.
- Ferramentas perigosas continuam escondidas em Configurações avançadas.
- Mantém mesas, vendas, estoque, relatórios, DEMO/PRO, guia com voz, multi-celular offline, exportação/importação e consolidação de atendente.

## Como subir no GitHub

Envie o conteúdo interno deste ZIP para a raiz do repositório.

A raiz deve conter:

```text
app/
.github/
build.gradle
settings.gradle
gradle.properties
README.md
guia.html
scripts/
```

Não envie uma pasta envolvendo esses arquivos.

## Artefato esperado

```text
Controle-Bar-v1.5.0-UX-10-SEGURANCA-APK
```

## Segurança operacional

O atendente usa somente atendimento, mesas, resumo e envio de movimento. Para virar gestor é obrigatório digitar o PIN administrativo.

O gestor usa o celular base, recebe dados do atendente, consolida relatórios e controla estoque oficial.
