# Controle do Bar — Android Nativo v1.2.1

Evolução direta da base funcional v1.1.3. O aplicativo continua usando o mesmo `applicationId` e o mesmo banco SQLite, portanto a atualização preserva produtos, estoque, vendas e relatórios já existentes.

## Recursos existentes preservados
- Dashboard com vendas e faturamento do dia.
- Cadastro, edição e arquivamento de produtos.
- Entrada e ajuste de estoque.
- Carrinho e baixa automática de estoque.
- Relatórios diários, histórico e lista do que comprar.
- Exportação e importação completa em JSON.
- Interface Android nativa e offline.

## Novidades v1.2.1
- DEMO de 3 dias iniciada no primeiro produto cadastrado.
- Se a atualização encontrar produtos já cadastrados, a DEMO começa no primeiro uso desta versão.
- Bloqueio após o prazo sem apagar nenhum dado.
- Tela de desbloqueio com senha administrativa.
- Ativação PRO gravada no aparelho e independente do banco SQLite.
- Guia HTML com voz nativa Android sincronizada: a etapa só avança quando a narração termina.
- Ícone profissional do Controle do Bar para a tela do celular.

## Gerar o APK no GitHub
1. Envie o conteúdo interno deste ZIP para a raiz do repositório.
2. Abra **Actions > Gerar APK Android**.
3. Execute **Run workflow**.
4. Baixe o artefato `Controle-Bar-v1.2.1-DEMO-PRO-APK`.

## Atualização sem perder dados
Instale o APK novo por cima do anterior. Não altere o `applicationId` e não desinstale o aplicativo antes de atualizar, pois a desinstalação remove o banco local do Android.


## Assinatura estável e preservação de dados
A partir da v1.2.1 o APK usa uma chave estável incluída no projeto. Isso permite instalar versões futuras por cima desta sem apagar o SQLite.

A v1.1.3 anterior foi assinada por uma chave temporária do GitHub Actions. Antes da primeira instalação da v1.2.1:
1. Abra a v1.1.3 e exporte o backup JSON.
2. Instale a v1.2.1. Se o Android exigir desinstalar a anterior, desinstale somente depois de exportar.
3. Importe o JSON na v1.2.1.
4. Nas próximas atualizações, não será mais necessário desinstalar.


## Correção v1.2.1 — recursos do ícone

O workflow limpa automaticamente arquivos residuais sem extensão dentro das pastas `mipmap-*` antes da compilação. Isso impede a falha `The file name must end with .xml or .png`, inclusive quando o GitHub cria arquivos extras chamados `1` ou `2` durante um upload manual.


## v1.3.0 - Mesas e relatórios visuais

- Adiciona Mesa 1 a Mesa 12.
- Permite abrir atendimento, adicionar itens e fechar a conta.
- Exporta relatório diário em HTML.
- Exporta lista de compras em HTML.
- Mantém DEMO/PRO, SQLite local, backup JSON e importação completa.

## v1.4.0 — Multi-celular offline

Esta versão permite trabalhar com um celular do gestor e um celular do atendente sem internet.

Fluxo:
1. No celular do gestor, abra Mais > Configurar este celular > GESTOR.
2. No celular do atendente, abra Mais > Configurar este celular > ATENDENTE.
3. O atendente usa mesas/vendas normalmente.
4. Ao fechar uma mesa, pode tocar em Fechar e enviar.
5. O app gera um JSON de movimento do dia.
6. O atendente envia esse arquivo por WhatsApp, Nearby Share, Bluetooth, e-mail ou Drive.
7. No celular do gestor, abra Mais > Receber e juntar dados do atendente.
8. Selecione o JSON recebido.
9. O app adiciona as vendas novas, evita duplicar vendas já importadas e aplica a baixa no estoque oficial.

A função Restaurar backup completo continua disponível, mas ela substitui todos os dados. Para juntar celulares, use apenas Receber e juntar dados do atendente.

## v1.4.1 — Usabilidade por perfil

Esta versão reorganiza o app para o uso real de bar com dois tipos de aparelho:

### Celular do gestor
Menu inferior:
- Início
- Mesas
- Estoque
- Relatórios
- Mais

Foco do gestor:
- estoque oficial;
- relatórios consolidados;
- receber e juntar movimento do atendente;
- lista de compras;
- backup completo somente em Configurações avançadas.

### Celular do atendente
Menu inferior:
- Início
- Mesas
- Resumo
- Enviar
- Mais

Foco do atendente:
- abrir mesa;
- adicionar itens;
- fechar atendimento;
- ver resumo do movimento local;
- enviar movimento do dia ao gestor.

### Segurança operacional
As opções perigosas ficam escondidas:
- Exportar backup completo;
- Restaurar backup completo.

Elas ficam em **Mais > Configurações avançadas**.

Para juntar dois celulares, use sempre:

**Receber e juntar dados do atendente**

Não use **Restaurar backup completo** para juntar celulares, porque essa opção substitui o banco atual.
