<?php
/**
 * Envio do formulário de contato (sem serviços de terceiros).
 *
 * CONFIGURAÇÃO (edite apenas estas duas linhas):
 *   DESTINO   : email que recebe as mensagens.
 *   REMETENTE : email de uma caixa que EXISTE no domínio do site (ex.: contato@seudominio.com.br).
 *               Deixe vazio para usar noreply@ + domínio atual. Caixa real melhora a entrega.
 */
const DESTINO   = 'mreis.santos88@gmail.com'; // TESTE. Trocar pelo email do cliente antes de publicar.
const REMETENTE = '';

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

function resposta(int $status, bool $ok, string $mensagem): void {
    http_response_code($status);
    echo json_encode(['ok' => $ok, 'mensagem' => $mensagem], JSON_UNESCAPED_UNICODE);
    exit;
}

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    resposta(405, false, 'Método não permitido.');
}

// Aceita apenas requisições vindas do próprio site
$host = preg_replace('/:\d+$/', '', $_SERVER['HTTP_HOST'] ?? '');
$origem = $_SERVER['HTTP_ORIGIN'] ?? ($_SERVER['HTTP_REFERER'] ?? '');
if ($origem !== '') {
    $hostOrigem = parse_url($origem, PHP_URL_HOST) ?: '';
    if (strcasecmp($hostOrigem, $host) !== 0) {
        resposta(403, false, 'Origem não permitida.');
    }
}

// Anti spam: campo armadilha e tempo mínimo de preenchimento
if (!empty($_POST['website'])) {
    resposta(200, true, 'Mensagem enviada. Obrigado!'); // finge sucesso para o robô
}
$aberto = (int)($_POST['t'] ?? 0);
if ($aberto > 0 && (time() - $aberto) < 3) {
    resposta(429, false, 'Aguarde alguns segundos e tente novamente.');
}

// Limite por IP: 1 mensagem a cada 30 s e no máximo 5 por hora
$ip = $_SERVER['REMOTE_ADDR'] ?? '0';
$arq = sys_get_temp_dir() . '/ae_form_' . md5($ip) . '.json';
$agora = time();
$hist = [];
if (is_file($arq)) {
    $hist = json_decode((string)@file_get_contents($arq), true) ?: [];
    $hist = array_values(array_filter($hist, fn($t) => $agora - $t < 3600));
}
if (count($hist) >= 5 || ($hist && $agora - end($hist) < 30)) {
    resposta(429, false, 'Muitas tentativas. Tente novamente mais tarde.');
}

// Validação e limpeza
$limpa = fn(string $s): string => trim(preg_replace('/[\x00-\x1F\x7F]+/', ' ', $s));
$nome = $limpa((string)($_POST['nome'] ?? ''));
$email = $limpa((string)($_POST['email'] ?? ''));
$texto = trim(str_replace("\r", '', (string)($_POST['mensagem'] ?? '')));

if ($nome === '' || mb_strlen($nome) > 100) {
    resposta(422, false, 'Informe seu nome.');
}
if (!filter_var($email, FILTER_VALIDATE_EMAIL) || mb_strlen($email) > 150) {
    resposta(422, false, 'Informe um email válido.');
}
if ($texto === '' || mb_strlen($texto) > 3000) {
    resposta(422, false, 'Escreva uma mensagem de até 3000 caracteres.');
}

// Monta o email (valores do usuário nunca vão para cabeçalhos sem limpeza)
$dominio = preg_replace('/^www\./i', '', $host);
$de = REMETENTE !== '' ? REMETENTE : 'noreply@' . ($dominio ?: 'localhost');
$assunto = 'Contato pelo site Administração Emocional';
$corpo = "Nova mensagem pelo site:\n\n"
       . "Nome: $nome\n"
       . "Email: $email\n\n"
       . "Mensagem:\n$texto\n";

$cabecalhos = [
    'From: ' . mb_encode_mimeheader('Site Administração Emocional', 'UTF-8') . " <$de>",
    'Reply-To: ' . mb_encode_mimeheader($nome, 'UTF-8') . " <$email>",
    'MIME-Version: 1.0',
    'Content-Type: text/plain; charset=UTF-8',
    'Content-Transfer-Encoding: 8bit',
    'X-Mailer: PHP/' . PHP_VERSION,
];

$modoTeste = getenv('FORM_DRY_RUN') === '1';
$enviado = $modoTeste ? true : @mail(DESTINO, mb_encode_mimeheader($assunto, 'UTF-8'), $corpo, implode("\r\n", $cabecalhos), '-f' . $de);

if (!$enviado) {
    resposta(500, false, 'Não foi possível enviar agora. Tente novamente em instantes.');
}

$hist[] = $agora;
@file_put_contents($arq, json_encode($hist), LOCK_EX);
resposta(200, true, 'Mensagem enviada! Responderemos em breve.');
