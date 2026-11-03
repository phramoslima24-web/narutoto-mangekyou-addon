# Narutoto - Mangekyou + Izanagi Addon 3.0 (Forge 1.20.1)

Requer **Narutoto 0.4.1+**. Nao precisa do jar do Narutoto para compilar.

## Como funciona
1. Os itens **Mangekyou do Itachi / Shisui / Madara** (aba criativa Narutoto) agora sao **implantes**: botao direito = chat mostra *"Implante concluido!"* e o item e consumido (no criativo nao).
2. Depois, ao ativar o Mangekyou do jeito normal do mod (B e seta para cima), o chat avisa qual olho despertou.
3. No **menu de dojutsu** aparecem botoes ao lado: *Aprender: <jutsu>*. Isso coloca o jutsu no slot escolhido, igual aos jutsus do mod.
4. Selecione o jutsu e use com a tecla de soltar jutsu do Narutoto.
5. **Teclas de reserva** (Controles > "Narutoto - Mangekyou Addon"): **G** usa o jutsu do implante (o selecionado no menu do Narutoto, ou o escolhido com **H**), **H** troca o jutsu do implante. Funcionam mesmo sem aprender no menu. Se algo falhar, o chat diz o motivo.
6. **Olhos:** com o Mangekyou ativo (estagio 2), os olhos do implante (Itachi, Shisui ou Madara) aparecem no rosto, tambem para outros jogadores.

| Implante | Jutsus extras (o Susanoo e o do proprio mod) |
|---|---|
| Itachi | Novo Amaterasu, Tsukuyomi |
| Shisui | Novo Amaterasu, Kotoamatsukami |
| Madara | Olho esquerdo (voltar 2 s), Olho direito (parar o tempo) |

**Novo Amaterasu:** fogo de verdade (bloco "Fogo Preto"), que nao se espalha, nao some sozinho e a chuva nao apaga. **Shift + usar o jutsu** apaga todos os fogos pretos.

## Gerar o .jar
- **GitHub Actions:** suba esta pasta num repositorio, rode o workflow `build-jar` e baixe o artefato.
- **Local:** JDK 17 + Gradle 8.1.1, `gradle build`; jar em `build/libs`.

## Limites conhecidos
- Os olhos sao desenhados por cima do Mangekyou do mod (mesma geometria); no estagio 3 (Eternal) nao mudam.
- Codigo nao foi compilado nem testado no jogo. Os dados do Narutoto sao lidos por reflexao (`Bridge.java`); se algo falhar, o log mostra `[narutoto_mangekyou_izanagi]`.
- Fogo preto em chunks descarregados so e apagado quando o chunk estiver carregado.

## Melhorias recentes
- **Auxilio de mira**: Novo Amaterasu, Tsukuyomi e Kotoamatsukami agora pegam o alvo mais proximo da sua linha de visao mesmo sem acertar exatamente nele.
- **Criativo** ignora recarga dos jutsus.
- Tsukuyomi e Kotoamatsukami nao afetam jogadores em criativo/espectador.
- Particulas extras no Tsukuyomi.


## Novidade 3.0: Izanagi e Izanami (nos 3 Mangekyou: Itachi, Shisui e Madara)
Cada implante agora tem 4 jutsus extras no menu de dojutsu: **Izanagi (olho esquerdo/direito)** e **Izanami (olho esquerdo/direito)**.

- **Izanagi:** fica armado por 2 minutos. Se voce *morrer* nesse tempo, a morte e anulada (vida cheia, efeitos ruins removidos, volta ao ponto de nascimento se cair no void) e **o olho escolhido fica cego**. Se o tempo acabar sem morrer, nada e cobrado. `/kill` nao e anulado. Recarga: 10 min.
- **Izanami:** prende o alvo na mira por 20 s (puxado de volta ao ponto, cego, sem forca, nao ataca). **O olho escolhido fica cego na hora.** Recarga: 5 min.
- **Cegueira:** o olho cego aparece palido no rosto (tambem para outros jogadores) e a **metade da tela daquele lado fica preta** (esquerdo = metade esquerda; os dois = tela toda). Jutsus que exigem o olho cego nao funcionam (Madara: voltar o tempo = olho esquerdo, parar o tempo = direito); com os dois cegos nenhum jutsu do addon funciona.
- **Cura:** a UNICA forma e implantar de novo o **mesmo Mangekyou** do olho que cegou. Outro Mangekyou e recusado enquanto voce estiver cego (e o item nao e gasto). A cegueira sobrevive a morte.
- **Como pegar o olho de novo:** o **Itachi NPC** do Narutoto larga o *Mangekyou do Itachi* quando um jogador o mata. Shisui e Madara nao tem NPC no Narutoto 0.4.1, entao so pela aba criativa/comandos (`/give @s narutoto_mangekyou_izanagi:mangekyou_shisui`).
- Izanami tambem cega o olho usado (como no anime). Para mudar, ajuste `Ability.costsEye()` / `IzanamiHandler.cast`.

IDs diferentes do addon antigo (`narutoto_mangekyou_izanagi`), entao os dois nao entram em conflito, mas **nao use os dois juntos** (os itens e jutsus se repetem).

Nao compilado nem testado no jogo. Se os olhos aparecerem trocados no rosto, mude `SWAP_EYE_SIDES` em `EyeLayer.java`.
