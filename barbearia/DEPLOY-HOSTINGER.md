# Colocando no ar na Hostinger

> **Importante:** os planos de hospedagem compartilhada da Hostinger (Single, Premium, Business, Cloud)
> rodam apenas PHP/WordPress e **não executam Java**. É preciso um **VPS** (o KVM 1 já é suficiente).

## 1. Criar o VPS

1. No hPanel: **VPS → Contratar/Configurar**. Escolha o template **Ubuntu 24.04 com Docker**
   (em "Aplicativos", procure por *Docker*).
2. Defina a senha de root e anote o **IP do servidor**.

## 2. Apontar o domínio

No hPanel: **Domínios → seu domínio → DNS / Nameservers**. Crie um registro:

| Tipo | Nome | Aponta para | TTL |
|---|---|---|---|
| A | `agendar` (ou `@` para o domínio principal) | IP do VPS | 300 |

Pode levar de alguns minutos a algumas horas para propagar. Confira com `ping agendar.seudominio.com.br`.

## 3. Instalar a aplicação no VPS

Pelo terminal do seu computador (ou pelo **Terminal do navegador** no hPanel):

```bash
ssh root@IP_DO_VPS

# Se escolheu um template sem Docker:
curl -fsSL https://get.docker.com | sh

git clone -b claude/barbershop-booking-payment-j20sj7 https://github.com/Msantos88/Msantos88.git
cd Msantos88/barbearia
cp .env.exemplo .env
nano .env          # preencha DOMINIO, senhas e nome da barbearia (Ctrl+O, Enter, Ctrl+X)

docker compose up -d --build
docker compose logs -f app   # aguarde "Started BarbeariaApplication" (Ctrl+C para sair)
```

> Se o repositório for privado, o `git clone` vai pedir usuário e um *Personal Access Token* do GitHub.

Abra `https://agendar.seudominio.com.br` — o HTTPS é configurado automaticamente.
Painel: `https://agendar.seudominio.com.br/admin.html`.

## 4. Testar online com o pagamento simulado

Com `PAGAMENTO_PROVEDOR=simulador`, faça agendamentos pelo celular e use o botão
"Simular pagamento". O navegador vai pedir o **login do painel** (usuário e senha do `.env`): só a barbearia
consegue simular pagamentos. Assim você valida horários, telas e painel antes de mexer com dinheiro.

> Cada telefone/e-mail só pode ter uma reserva aguardando pagamento por vez, e cada conexão pode criar até
> 5 reservas por hora. Para testes repetidos, simule o pagamento ou use telefones/e-mails diferentes.

## 5. Ligar o Mercado Pago

1. Acesse https://www.mercadopago.com.br/developers/panel → **Criar aplicação**
   (tipo *Pagamentos online*, produto *Checkout Transparente / API*).
2. Garanta que a conta tem uma **chave PIX** cadastrada (app do Mercado Pago → Pix → Minhas chaves).
3. Em **Credenciais de produção**, copie o **Access Token** (`APP_USR-...`).
4. Em **Webhooks → Configurar notificações**:
   - URL de produção: `https://agendar.seudominio.com.br/api/pagamentos/webhook/mercadopago`
   - Evento: **Pagamentos**
   - Salve e copie a **Assinatura secreta**.
5. No VPS, edite o `.env`:

   ```
   PAGAMENTO_PROVEDOR=mercadopago
   MERCADOPAGO_ACCESS_TOKEN=APP_USR-...
   MERCADOPAGO_WEBHOOK_SECRET=...
   MERCADOPAGO_NOTIFICATION_URL=https://agendar.seudominio.com.br/api/pagamentos/webhook/mercadopago
   ```

   e reinicie: `docker compose up -d`.

6. **Teste real com valor baixo:** o PIX do ambiente de teste do Mercado Pago não pode ser pago de verdade,
   então o jeito mais simples é testar em produção com um serviço barato. Crie temporariamente um serviço de
   R$ 2,00 (sinal de R$ 1,00):

   ```bash
   docker compose exec db psql -U barbearia -c \
     "insert into servico (nome, descricao, preco, duracao_minutos) values ('Teste', 'Teste de pagamento', 2.00, 30);"
   ```

   Agende, pague o PIX de R$ 1,00 pelo app do banco e veja a tela mudar para "Horário garantido!".
   Depois desative o serviço de teste:

   ```bash
   docker compose exec db psql -U barbearia -c "update servico set ativo = false where nome = 'Teste';"
   ```

## Incluir barbeiros

Pelo painel: `https://agendar.seudominio.com.br/admin.html` → aba **Barbeiros** → digite o nome → **Incluir**.
Com mais de um barbeiro ativo, o cliente passa a escolher com quem quer ser atendido.

## Comandos úteis

```bash
docker compose logs -f app                 # ver logs
docker compose restart app                 # reiniciar
git pull && docker compose up -d --build   # atualizar para nova versão
docker compose exec db pg_dump -U barbearia barbearia > backup.sql   # backup do banco
```

## Problemas comuns

- **Site não abre / erro de certificado:** o DNS ainda não propagou ou as portas 80/443 estão bloqueadas.
  No hPanel, em **VPS → Firewall**, libere as portas 80 e 443 (e 22 para SSH).
- **App reinicia em loop com erro de `ADMIN_SENHA`:** a senha do painel precisa ter 10+ caracteres e não pode
  ser a de exemplo (`troque-...`). Veja `docker compose logs app`.
- **Painel responde "Muitas tentativas de login":** foram 10 senhas erradas seguidas daquele IP; aguarde 15 minutos.
- **Pagou e não confirmou:** confira os logs (`docker compose logs app | grep -i pagamento`) e a URL do webhook
  no painel do Mercado Pago. Mesmo sem webhook, o sistema consulta pagamentos pendentes a cada minuto.
