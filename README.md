# Narutoto - Mangekyou Addon (Forge 1.20.1)

Addon para **Narutoto: Ninja World 0.4.1**. Adiciona 3 itens na aba criativa **Narutoto** (a mesma onde ficam os Eyeballs):

| Item | Habilidades |
|---|---|
| Mangekyou do Itachi | Susanoo, Amaterasu, Tsukuyomi |
| Mangekyou do Shisui | Susanoo, Amaterasu, Kotoamatsukami |
| Mangekyou do Madara | Olho esquerdo: volta 2 s no tempo / Olho direito: para o tempo (exceto o jogador) |

**Uso:** botao direito = usar a habilidade selecionada. **Shift + botao direito** = trocar de habilidade.

## Como gerar o .jar (nao precisa do jar do Narutoto para compilar)

### Opcao A - sem instalar nada (GitHub Actions)
1. Crie um repositorio no GitHub e suba todo o conteudo desta pasta.
2. Aba **Actions** > workflow **build-jar** > rode (ou ele roda sozinho no push).
3. Baixe o artefato `narutoto-mangekyou-addon-jar`: la dentro esta o `.jar`.

### Opcao B - local
1. Instale o **JDK 17** e o **Gradle 8.1.1** (ou copie `gradlew`, `gradlew.bat` e a pasta `gradle/` do MDK do Forge 1.20.1 para ca).
2. Na pasta do projeto: `gradle build` (ou `./gradlew build`).
3. O jar fica em `build/libs/narutoto-mangekyou-addon-1.0.0.jar`.

Coloque o .jar na pasta `mods` junto com o `narutoto-0_4_1-forge.jar`, e com **GeckoLib**, que o Narutoto usa.

## Detalhes e limites
- Texturas sao placeholders gerados (32x32). Troque em `src/main/resources/assets/narutoto_mangekyou/textures/item/`.
- Parar o tempo dura 10 s, recarga 60 s. Mobs ficam congelados; projeteis/itens/TNT param; outros jogadores ficam imobilizados e sem interagir.
- Voltar o tempo so tem historico se o jogador estiver com o Mangekyou do Madara no inventario ha pelo menos ~0,5 s. Nao revive quem morreu.
- As habilidades sao implementadas pelo proprio addon (nao reaproveitam as animacoes/entidades do Susanoo do Narutoto), por isso funcionam sem depender dos internos do mod.
- Codigo nao foi compilado/testado no jogo por quem gerou (ambiente sem Forge e sem internet): se o build acusar erro, mande a mensagem.
