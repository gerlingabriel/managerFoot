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

// Janela do clube por cima da tela (modelo 14e): fechar volta à tela; depois de uma compra, recarrega
// para o caixa e o plantel aparecerem certos.
(function () {
	var comprou = false;
	document.addEventListener('htmx:afterSettle', function () {
		if (document.querySelector('[data-recarregar]')) comprou = true;
	});
	document.addEventListener('click', function (e) {
		var fechar = e.target.closest('[data-fechar-janela]');
		if (!fechar) return;
		e.preventDefault();
		if (comprou) location.reload();
		else fechar.closest('.janela-fundo').remove();
	});
})();

// Som dos gols no quadro da rodada (SPEC §10.2 e §10.4): o servidor diz os gols do clube do usuário;
// quando um número sobe e o som está ligado, toca um som curto, um para o gol feito e outro para o
// sofrido. Gerado pelo Web Audio, sem arquivo. O navegador só libera o áudio depois de um clique na
// página.
(function () {
	var contexto = null;
	var anterior = null;

	function audio() {
		if (!contexto && (window.AudioContext || window.webkitAudioContext)) {
			contexto = new (window.AudioContext || window.webkitAudioContext)();
		}
		return contexto;
	}

	// Algumas vozes quase iguais passam por um filtro em volta de uma vogal: parece gente, não apito.
	function coro(de, para, vogal, duracao, ganho) {
		var ctx = audio();
		if (!ctx) return;
		var agora = ctx.currentTime;
		var volume = ctx.createGain();
		var filtro = ctx.createBiquadFilter();
		filtro.type = 'bandpass';
		filtro.frequency.value = vogal;
		filtro.Q.value = 1.2;
		volume.gain.setValueAtTime(0.0001, agora);
		volume.gain.exponentialRampToValueAtTime(ganho, agora + 0.04);
		volume.gain.setValueAtTime(ganho, agora + duracao * 0.6);
		volume.gain.exponentialRampToValueAtTime(0.0001, agora + duracao);
		filtro.connect(volume).connect(ctx.destination);
		[-9, 0, 7].forEach(function (desvio) {
			var osc = ctx.createOscillator();
			osc.type = 'sawtooth';
			osc.detune.value = desvio;
			osc.frequency.setValueAtTime(de, agora);
			osc.frequency.exponentialRampToValueAtTime(para, agora + duracao);
			osc.connect(filtro);
			osc.start(agora);
			osc.stop(agora + duracao);
		});
	}

	function golFeito() {
		coro(330, 520, 750, 0.4, 0.5);
	}

	function golSofrido() {
		coro(190, 95, 380, 0.45, 0.6);
	}

	function conferir() {
		var quadro = document.getElementById('quadro');
		if (!quadro || quadro.dataset.som === undefined) return;
		var atual = { pro: Number(quadro.dataset.golsPro || 0), contra: Number(quadro.dataset.golsContra || 0) };
		if (anterior && quadro.dataset.som === 'true') {
			if (atual.pro > anterior.pro) golFeito();
			else if (atual.contra > anterior.contra) golSofrido();
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
