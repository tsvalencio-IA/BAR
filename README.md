# Controle BAR v1.6.0 — UX 20/10: gestor, atendente, PDF e compras por fornecedor

Projeto Android nativo para GitHub Actions gerar APK.

## Destaques da v1.6.0

- Base preservada da linha funcional v1.5.0.
- Atendente sem aba **Mais**: usa apenas **Início**, **Mesas**, **Pedidos** e **Enviar**.
- Perfil **Gestor/Atendente** minimizável no painel.
- PIN do gestor é cadastrado pelo próprio gestor na primeira configuração administrativa.
- Senha PRO não é usada como PIN administrativo.
- Modo gestor continua protegido por PIN.
- Tema escuro e tema claro dentro do app.
- Envio do atendente via compartilhamento nativo Android: Bluetooth, Nearby Share/Wi‑Fi Direct, WhatsApp, Drive, e-mail etc.
- Produtos agora possuem campo **Fornecedor**.
- Lista de compras separada por fornecedor: Anbev, Cristal, distribuidora etc.
- PDF profissional de vendas do dia.
- PDF profissional de compras geral.
- PDF profissional de compras por fornecedor.
- Compartilhamento direto de PDF para dono do bar ou fornecedores.
- Mantém mesas, vendas, estoque, relatórios, DEMO/PRO, guia com voz, SQLite offline, backup/importação e consolidação de atendente.

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
Controle-Bar-v1.6.0-20-10-GESTOR-ATENDENTE-PDF-APK
```

## Segurança operacional

O atendente usa somente atendimento, mesas, pedidos e envio de movimento. Ele não acessa backup completo, restauração ou configurações administrativas.

O gestor usa o celular base, recebe dados do atendente, consolida relatórios, controla estoque oficial e exporta PDFs profissionais.
