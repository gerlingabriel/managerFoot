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

// Som dos gols no quadro da rodada (SPEC §10.2 e §10.4): o servidor diz quantos gols a divisão em
// jogo já teve; quando o número sobe e o som está ligado, toca um apito curto. Gerado pelo Web
// Audio, sem arquivo. O navegador só libera o áudio depois de um clique na página.
(function () {
	var contexto = null;
	var anterior = null;

	function audio() {
		if (!contexto && (window.AudioContext || window.webkitAudioContext)) {
			contexto = new (window.AudioContext || window.webkitAudioContext)();
		}
		return contexto;
	}

	function apito() {
		var ctx = audio();
		if (!ctx) return;
		[0, 0.22].forEach(function (atraso) {
			var osc = ctx.createOscillator();
			var volume = ctx.createGain();
			osc.type = 'triangle';
			osc.frequency.value = 1760;
			volume.gain.setValueAtTime(0.0001, ctx.currentTime + atraso);
			volume.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + atraso + 0.02);
			volume.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + atraso + 0.18);
			osc.connect(volume).connect(ctx.destination);
			osc.start(ctx.currentTime + atraso);
			osc.stop(ctx.currentTime + atraso + 0.2);
		});
	}

	function conferir() {
		var quadro = document.getElementById('quadro');
		if (!quadro || quadro.dataset.gols === undefined) return;
		var atual = { gols: Number(quadro.dataset.gols), divisao: quadro.dataset.divisao };
		if (anterior && anterior.divisao === atual.divisao && atual.gols > anterior.gols && quadro.dataset.som === 'true') {
			apito();
		}
		anterior = atual;
	}

	document.addEventListener('click', function () {
		var ctx = audio();
		if (ctx && ctx.state === 'suspended') ctx.resume();
	});
	document.addEventListener('DOMContentLoaded', conferir);
	document.addEventListener('htmx:afterSettle', conferir);
})();
