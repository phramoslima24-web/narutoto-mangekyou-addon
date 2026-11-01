# Narutoto - Mangekyou Addon 2.0 (Forge 1.20.1)

Requer **Narutoto 0.4.1+**. Nao precisa do jar do Narutoto para compilar.

## Como funciona
1. Os itens **Mangekyou do Itachi / Shisui / Madara** (aba criativa Narutoto) agora sao **implantes**: botao direito = chat mostra *"Implante concluido!"* e o item e consumido (no criativo nao).
2. Depois, ao ativar o Mangekyou do jeito normal do mod (B e seta para cima), o chat avisa qual olho despertou.
3. No **menu de dojutsu** aparecem botoes ao lado: *Aprender: <jutsu>*. Isso coloca o jutsu no slot escolhido, igual aos jutsus do mod.
4. Selecione o jutsu e use com a tecla de soltar jutsu do Narutoto.

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
- Textura do olho no rosto: continua a do mod (nao troca por implante).
- Codigo nao foi compilado nem testado no jogo. Os dados do Narutoto sao lidos por reflexao (`Bridge.java`); se algo falhar, o log mostra `[narutoto_mangekyou]`.
- Fogo preto em chunks descarregados so e apagado quando o chunk estiver carregado.
