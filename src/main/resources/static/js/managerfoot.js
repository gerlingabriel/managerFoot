// Comportamentos simples das telas. Nenhuma regra de jogo fica aqui.

// Setas da faixa com rolagem lateral (novo jogo): rolam quase uma largura visível.
document.addEventListener('click', function (e) {
	var seta = e.target.closest('[data-rolar]');
	if (!seta) return;
	var faixa = document.getElementById(seta.getAttribute('aria-controls'));
	if (faixa) faixa.scrollBy({ left: Number(seta.dataset.rolar) * faixa.clientWidth * 0.8, behavior: 'smooth' });
});

// Formulário que cria algo: o segundo clique não envia de novo enquanto o primeiro não volta.
document.addEventListener('submit', function (e) {
	var form = e.target;
	if (!form.hasAttribute('data-envio-unico')) return;
	if (form.dataset.enviado) { e.preventDefault(); return; }
	form.dataset.enviado = 'sim';
	form.querySelectorAll('button[type=submit]').forEach(function (b) { b.setAttribute('aria-disabled', 'true'); });
});

// Seleção que grava ao mudar (batedor de pênalti): envia o próprio formulário.
document.addEventListener('change', function (e) {
	if (e.target.matches('select[data-enviar-ao-mudar]')) e.target.form.requestSubmit();
});
