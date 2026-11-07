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

// Campo que grava ao mudar (batedor de pênalti, arquivo do jogo): envia o próprio formulário.
document.addEventListener('change', function (e) {
	if (e.target.matches('[data-enviar-ao-mudar]')) e.target.form.requestSubmit();
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
// quando um número sobe e o som está ligado, toca a torcida: comemoração no gol feito, vaia no sofrido
// (gravações em /audio, créditos em docs/creditos-sons.md). Ficam fora do #quadro, que o polling troca.
(function () {
	var anterior = null;
	var somGolFeito = new Audio('/audio/gol-feito.wav');
	var somGolSofrido = new Audio('/audio/gol-sofrido.wav');
	somGolFeito.volume = 0.5;
	somGolSofrido.volume = 0.5;

	function tocar(som) {
		som.currentTime = 0;
		// o navegador pode recusar o áudio antes de um clique na página
		som.play().catch(function () { });
	}

	function golFeito() {
		tocar(somGolFeito);
	}

	function golSofrido() {
		tocar(somGolSofrido);
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

	document.addEventListener('DOMContentLoaded', conferir);
	document.addEventListener('htmx:afterSettle', conferir);
})();

// Botão "Copiar" do link do convite (modelo 18c): copia o texto do campo indicado e avisa no próprio botão.
document.addEventListener('click', function (e) {
	var botao = e.target.closest('[data-copiar]');
	if (!botao) return;
	var campo = document.getElementById(botao.dataset.copiar);
	if (!campo) return;
	var avisar = function () {
		botao.textContent = 'Copiado';
		setTimeout(function () { botao.textContent = 'Copiar'; }, 2000);
	};
	if (navigator.clipboard && window.isSecureContext) {
		navigator.clipboard.writeText(campo.value).then(avisar);
	} else {
		// fora de HTTPS o navegador não libera a área de transferência: seleciona para o Ctrl+C
		campo.select();
		document.execCommand('copy');
		avisar();
	}
});

// Atalho do Enter (plantel e leilões): segue pelo botão principal da tela, marcado com data-enter, quando
// não há janela aberta e o foco não está num campo nem num botão ou link escolhido pelo teclado (Tab);
// o botão que ficou com o foco depois de um clique do mouse (uma tática, T/B/–) não conta. Num campo
// de um formulário que tem botão principal (o lance do leilão), o Enter usa esse botão, não o primeiro
// do formulário. O botão desligado (escalação incompleta) não é acionado: o servidor decide tudo.
(function () {
	// o Chrome passa o botão para :focus-visible no próprio keydown; por isso guarda como o foco chegou
	var focoPeloTeclado = false;
	document.addEventListener('pointerdown', function () { focoPeloTeclado = false; }, true);
	document.addEventListener('keydown', function (e) {
		if (e.key === 'Tab') { focoPeloTeclado = true; return; }
		if (e.key !== 'Enter' || e.isComposing || e.altKey || e.ctrlKey || e.metaKey || e.shiftKey) return;
		if (document.querySelector('[role=dialog], .janela-fundo')) return;
		var alvo = e.target;
		var principal;
		if (alvo.matches('input') && alvo.form) {
			principal = alvo.form.querySelector('[data-enter]');
			if (!principal) return;
		} else if (alvo.closest('select, textarea, input, [contenteditable]')
				|| (alvo.closest('button, a, summary') && focoPeloTeclado)) {
			return;
		} else {
			principal = document.querySelector('[data-enter]');
		}
		if (!principal || principal.disabled || principal.getAttribute('aria-disabled') === 'true') return;
		e.preventDefault();
		principal.click();
	});
})();
