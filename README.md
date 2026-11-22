# Narutoto - Mangekyou Addon 2.3 (Forge 1.20.1)

Requer **Narutoto 0.4.1+**. Nao precisa do jar do Narutoto para compilar.

## Como funciona
1. Os itens **Mangekyou do Itachi / Shisui / Madara** (aba criativa Narutoto) agora sao **implantes**: botao direito = chat mostra *"Implante concluido!"* e o item e consumido (no criativo nao).
2. Depois, ao ativar o Mangekyou do jeito normal do mod (B e seta para cima), o chat avisa qual olho despertou.
3. No **menu de dojutsu** aparecem botoes ao lado: *Aprender: <jutsu>*. Isso coloca o jutsu no slot escolhido, igual aos jutsus do mod.
4. Selecione o jutsu e use com a tecla de soltar jutsu do Narutoto.
5. **Teclas de reserva** (Controles > "Narutoto - Mangekyou Addon"): **G** usa o jutsu do implante (o selecionado no menu do Narutoto, ou o escolhido com **H**), **H** troca o jutsu do implante. Funcionam mesmo sem aprender no menu. Se algo falhar, o chat diz o motivo.
6. **Olhos:** com o Mangekyou ativo (estagio 2) aparecem os olhos antigos do implante e, no **Mangekyou Eterno (estagio 3, `if_sharigan == 3`)**, as texturas dos Mangekyous Eternos de **Madara, Itachi e Shisui** (`textures/entity/mangekyou_eternal_<personagem>[_blind].png`), tambem para outros jogadores.

| Implante | Jutsus extras (o Susanoo e o do proprio mod) |
|---|---|
| Itachi | Novo Amaterasu, Tsukuyomi, Izanagi e Izanami (olho esq./dir.) |
| Shisui | Novo Amaterasu, Kotoamatsukami, Izanagi e Izanami (olho esq./dir.) |
| Madara | Olho esquerdo (voltar 5 s), Olho direito (parar o tempo), Izanagi e Izanami (olho esq./dir.) |
| Rinne Sharingan | Tsukuyomi Infinito, Raizes da Shinju, Amenominaka (+ passivo) |

**Novo Amaterasu:** fogo de verdade (bloco "Fogo Preto"), que nao se espalha, nao some sozinho e a chuva nao apaga. **Shift + usar o jutsu** apaga todos os fogos pretos.

## Izanagi e Izanami (Itachi, Shisui e Madara)
Cada um existe para o olho **esquerdo** e para o **direito**; o olho usado fica **cego**.
- **Izanagi** (recarga 10 min): deixa um **corpo** (com a sua skin) deitado no chão e fica armado por **5 min**. Se você morrer nesse tempo, a morte é anulada: vida cheia, efeitos ruins removidos e você é **teleportado para o corpo**, mesmo em outra dimensão. Se o tempo passar sem morrer, o corpo some e nada é cobrado. `/kill` não é anulado.
- **Izanami** (recarga 5 min): mira no inimigo e usa: ele fica **imobilizado para sempre** (puxado de volta ao ponto, cego, sem força, sem atacar) **tomando dano contínuo** até morrer. Sobrevive a reiniciar o servidor.
- **Cegueira**: um olho cego deixa **metade da tela preta** (lado do olho usado, com faixa branca na borda) e os jutsus daquele olho ficam bloqueados. **Dois olhos cegos** = tela toda preta e as criaturas em volta (24 blocos) **brilham em branco**.
- **Cura**: só o **Mangekyou do MESMO personagem** que causou a cegueira (Madara cura Madara, Itachi cura Itachi, Shisui cura Shisui). Os outros dois são recusados e o item não é consumido. Devolve a visão dos dois olhos. A cegueira (e de quem é) sobrevive à morte, a sair do servidor e a trocar de dimensão. O Itachi do Narutoto (`narutoto:itachi_npc`) larga o olho do Itachi.
- Limitação: o brilho branco usa o efeito Glowing, então os outros jogadores também veem as criaturas brilhando enquanto você estiver cego dos dois olhos.

## Rinne Sharingan
- **Tsukuyomi Infinito** (recarga 5 min, 2000 de chakra): a noite, no Overworld, olhando para a lua (ceu aberto). Prende todos os seres vivos num raio de 64 blocos por 15 s (cego, lento, fraco, com fome; dano de "tortura" que nao mata sozinho). Quem lancou ganha regeneracao, e o chakra dos jogadores presos e sugado (10% do maximo por segundo) e passa para quem lancou. Para testar sem esperar a noite, mude `REQUIRE_MOON` para `false` em `RinneAbilities.java`.
- **Raizes da Shinju** (recarga 30 s, 500 de chakra): envolve em casulos as vitimas do Tsukuyomi Infinito (ou o alvo na mira e quem estiver colado nele) por 15 s; as raizes drenam vida da vitima (sem mata-la) e curam o dono.
- **Amenominaka** (recarga 2 min, 1000 de chakra): abre por 20 s uma dimensao aleatoria num raio de 12 blocos: **lava**, **gelo**, **gravidade aumentada** ou **tempestade de areia**. Quem lancou fica imune.
- **Poderes oculares combinados (passivo)**: visao noturna e 25% de chance de prever e desviar de golpes de outras entidades, enquanto o Rinne Sharingan estiver ativo (estagio 2).

## Mangekyou do Sasuke (drop do Sasuke do Narutoto) - 2.7.0
- Novo implante **Sasuke** (`narutoto_mangekyou:mangekyou_sasuke`), no mesmo sistema dos outros Mangekyous: botao direito = implante, aparece na aba criativa do Narutoto, jutsus no menu de dojutsu, teclas G/H, olhos desenhados por `EyeLayer`.
- **Drop:** ao derrotar o `narutoto:sasuke_npc` (jogador como autor), `EyeDrops` solta o Mangekyou do Sasuke com **25%** de chance (+5% por nivel de Pilhagem). Os drops originais do Sasuke (ex.: Sharingan Eyeball) nao foram alterados. Ajuste `SASUKE_DROP_CHANCE` / `SASUKE_LOOTING_BONUS` em `EyeDrops.java`.
- **Poderes:** Susanoo **original do Narutoto** (continua intacto: o Sasuke nao tem `SusanooKind`) + **Novo Amaterasu**. Sem Izanagi/Izanami, entao nao cega.
- **Visual:** icone e olhos (estagio 2) e olhos eternos (estagio 3) sao recortes das texturas originais do Narutoto (`mangekyou_sasuke.png`, `eternal_sasuke.png`, `mangekyou_sharingan_sasuke_icon.png`), no mesmo formato dos outros personagens.
- Nao foi compilado nem testado no jogo (sem Forge/rede no ambiente de edicao): gere o jar pelo workflow `build-jar`.

## Gerar o .jar
- **GitHub Actions:** suba esta pasta num repositorio, rode o workflow `build-jar` e baixe o artefato.
- **Local:** JDK 17 + Gradle 8.1.1, `gradle build`; jar em `build/libs`.

## Compatibilidade com o Narutoto 0.4.1 (conferida no jar)
O Narutoto guarda o **nome traduzido** do jutsu (`jutsu_key`, `learned_jutsu`, `jutsu_choosed_name`), nao a chave interna. O addon agora grava e le o nome traduzido; a chave antiga continua aceita. Variaveis usadas: `chakra`, `chakra_maximum`, `chakra_cost`, `if_sharigan`, `sharingan_position`, `learned_jutsu`, `jutsu_key`, `jutsu_choosed_name`.

## Limites conhecidos
- Os custos de chakra estao so nos jutsus do Rinne (`Ability.java`); os dos outros olhos continuam gratis. O Amenominaka cria "dominios" por efeitos, nao leva para uma dimensao de verdade.
- Os olhos sao desenhados por cima do Mangekyou do mod (mesma geometria); no estagio 3 (Eternal) usam as texturas eternas do addon.
- Codigo nao foi compilado nem testado no jogo. Os dados do Narutoto sao lidos por reflexao (`Bridge.java`); se algo falhar, o log mostra `[narutoto_mangekyou]`.
- Fogo preto em chunks descarregados so e apagado quando o chunk estiver carregado.

## Melhorias recentes
- **Auxilio de mira**: Novo Amaterasu, Tsukuyomi e Kotoamatsukami agora pegam o alvo mais proximo da sua linha de visao mesmo sem acertar exatamente nele.
- **Criativo** ignora recarga dos jutsus.
- Tsukuyomi e Kotoamatsukami nao afetam jogadores em criativo/espectador.
- Particulas extras no Tsukuyomi.

## Atualizacao 2.4 (Mangekyous Eternos, cegueira por personagem, Madara, Rewind 5 s)
- **Texturas eternas** (estagio 3) de Madara, Itachi e Shisui + versoes cegas; o olho cego mostra a textura cega do personagem dono da cegueira e o outro olho a normal. O Sharingan normal (estagio 1) nao e alterado.
- **Cegueira por personagem**: o addon grava de qual Mangekyou veio a cegueira (`mkaddon_blind_owner`, sincronizado no `SyncImplantPacket`). Saves antigos sem dono usam o implante atual (ou Itachi).
- **Rewind = exatamente 5 s** (`RewindHandler.REWIND_TICKS = 100`; historico de 101 fotos por entidade). O jutsu foi renomeado para "voltar 5 segundos"; o nome antigo continua aceito (`Ability.isLearnedIn`). O Madara nao perde vida ao voltar, apaga o fogo, ganha resistencia/regeneracao e os seres vivos ao redor (12 blocos) ficam lentos/enjoados por 2 s. Recarga mantida (10 s).
- **Parar o tempo** (Madara): 15 s (antes 10 s), onda de choque, congelados brilham, Madara ganha velocidade/forca/resistencia e causa +75% de dano em congelados. Recarga mantida (60 s).
- Nada foi compilado nem testado no jogo.

- **Correcao do Rewind**: so pode ser usado com os 101 registros seguidos (exatamente 100 ticks = 5 s); depois de usar, o historico e zerado e precisa acumular os 101 de novo. Buracos no historico (troca de dimensao, morte) reiniciam a contagem. Seres ao redor sem os 101 registros nao sao movidos.

## Atualizacao 2.5 - Susanoos exclusivos (Madara, Itachi, Shisui)
**Requer GeckoLib 4.x** (o Narutoto ja usa). Nao ha habilidade nova de ativar Susanoo: usa-se a **original do mod**.
Ao o mod invocar o Susanoo do Sasuke (costela, esqueleto, completo, perfeito), o addon olha o **implante equipado**
(`Implants.get`, sincronizado ao cliente) e troca pela versao do personagem, no mesmo estagio:

| Mangekyou | Susanoo | Habilidades (menu de dojutsu / tecla G e H) |
|---|---|---|
| Madara | azul-escuro | **Yasaka Magatama** (3 magatamas explosivos, cratera, onda de choque); **Susanoos nos clones** (liga/desliga) |
| Itachi | dourado | **Espelho de Yata** (escudo que reflete projeteis, absorve chakra, bloqueia golpes e dano elemental); **Joia Yasaka Magatama** (3 discos perfurantes) |
| Shisui | verde | **Tsukumo** (84 agulhas; com Katon recente ficam em chamas e +57% de dano); **Broca/Lanca** (liga; clique esquerdo = estocada que atravessa) |
| Sasuke / sem implante | original | nada muda |

- Cada personagem tem 3 tipos de entidade proprios, 4 estagios com **modelo, animacoes, textura e mascara de brilho proprios**
  (`geo/<personagem>_susanoo_<estagio>.geo.json`, `textures/entity/<personagem>_susanoo_<estagio>.png`).
- As habilidades so funcionam com o **Susanoo do mesmo personagem ativo** e custam chakra/recarga independentes (`Ability.java`).
- **Clones**: o mod nao tem "clone de madeira"; foi usado o **Clone das Sombras** (`clone_new` / `sage_clone`). Cada clone recebe um Susanoo (estagio completo) que o segue; some quando o clone morre/expira. O limite e o do mod (chakra + 5), no maximo 12.
- Fogo (Katon) compativel com o Tsukumo: Bola de Fogo, Grande Dragao de Fogo, Hosenka, Grande Rajada de Chamas, Grande Aniquilacao, Bomba de Oleo de Sapo, Ocultacao em Cinzas (lancados ate 5 s antes ou durante a rajada).
- Susanoos do addon nao sao salvos no disco e somem sozinhos (dono/clone/implante mudou, dono offline): sem entidades abandonadas.
- O Susanoo ORIGINAL do Sasuke continua existindo (invisivel) e mantem todos os poderes (montaria, espada, arco, punhos); o Susanoo do Mangekyou o acompanha como corpo visual e das habilidades novas.
- Nada foi compilado nem testado no jogo.

## Atualizacao 2.8 - Despertar aleatorio, olhos acumulados, barra automatica
- **Sorteio unico** (`Implants.awakenLottery`): no primeiro despertar do Mangekyou (`if_sharigan >= 2`) o jogador recebe, ao acaso, Sasuke, Madara, Itachi ou Shisui (`EyeSet.draw`, uniforme). Obito e Rinne nao entram. Resultado salvo em `mkaddon_awakened` + `mkaddon_eyes`; nunca sorteia de novo (morte, relog, reinicio). Quem implantar um olho por item ANTES de despertar nao sorteia (ja tem olho).
- **Transplante acumulativo**: `mkaddon_eyes` guarda todos os olhos. Implantar nunca remove nada; olho repetido e recusado e o item NAO e consumido (exceto a cura de cegueira, que continua exigindo o olho do mesmo personagem). O olho novo vira o **olho ativo** (aparencia, Susanoo do personagem). **Shift + tecla "trocar jutsu" (H)** alterna o olho ativo.
- **Uniao de jutsus**: `Implants.owns/abilities` somam os jutsus de todos os olhos, sem duplicar (Amaterasu, Izanagi e Izanami existem uma vez so). Qualquer um funciona com qualquer olho ativo (os de Susanoo exigem o Susanoo do proprio personagem ativo, como antes).
- **Barra original**: `Implants.grantBar` grava cada jutsu novo em `learned_jutsu` e no primeiro slot livre `jutsu_key1..20` do Narutoto e chama `JutsuSwichNameDisplayProcedure`. Nao cria barra nova, nao remove nada, so adiciona o que ainda nao foi aprendido (roda ao despertar/implantar e a cada 2 s com o Mangekyou ativo). Barra cheia: o jutsu fica aprendido e pode ser posto no slot pelo menu de dojutsu.
- **Drops configurados** (`config/narutoto_mangekyou-common.toml`): `narutoto:sasuke_npc` 25% (+5%/Pilhagem), `narutoto:itachi_npc` 100% (como antes). **O NarutoTo 0.4.1 nao tem entidade de Madara nem de Shisui**; `extraDrops` (vazia) permite mapear qualquer entidade real, ex.: `"narutoto:uchiha_npc_3|madara|0.03|0.01"`.
- Corrigido: "Tsukuyomi" era confundido com "Tsukuyomi Infinito" ao checar jutsus aprendidos.
- Nao compilado nem testado no jogo (ver relatorio).

## Correcao 2.8.1 - jutsus de Susanoo do Madara com outro olho ativo
Bug: depois de implantar o Itachi (que vira o olho ativo), os jutsus de Susanoo do Madara (Yasaka Magatama, Susanoos nos clones) diziam "Seu implante nao permite esse jutsu". Causa: `SusanooAbilities.require()` comparava com o olho ATIVO. Agora exige apenas TER o olho (`EyeSet.gate`): se o olho existe e outro esta ativo, o ativo e trocado para o dono do jutsu (o Susanoo do personagem reaparece em ~2-3 s; use o jutsu de novo); sem o olho continua negado. O menu de dojutsu lista os jutsus de todos os olhos (`SyncImplantPacket` agora envia a lista; protocolo de rede 2).
