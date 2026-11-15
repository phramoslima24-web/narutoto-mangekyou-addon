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
