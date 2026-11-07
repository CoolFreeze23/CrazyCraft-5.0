**Alpha release.** The second part of CrazyCraft 5.0's new story, *The Spilled Hoard*: the First Night and Week. It's for trying out early on your own computer or on a private server. The stable release is still v1.4.2.

*Versão alfa. A segunda parte da nova história do CrazyCraft 5.0, *O Tesouro Derramado*: a Primeira Noite e a Semana. Ela é para testar antes, no seu computador ou num servidor privado. A versão estável continua sendo a v1.4.2.*

## Before you play
- **Back up your worlds first.** Copy your `saves/` folder (or the server's world folder) somewhere safe.
- **Import the client zip as a new instance, or let an alpha instance update itself.** The setup program and the in-game update check only offer full releases, so v1.4.x installs stay on v1.4.x.
- **Not for the v1.4.x hub.** A v2.0.0-alpha.4 client can't join a v1.4.x server, or the other way round.
- **The first world can take a while.** Creating a first world can take up to about a minute on a handheld, while Shelf 9 and the Hunt Lodge look for a place to stand.

## Downloads
- `CrazyCraft5-Client.zip`: the client pack. Import it into Prism Launcher or MultiMC (Add Instance, then Import) and give it 8 to 12 GB of RAM. The first launch downloads the mods from their official pages. MCHeli still has to be downloaded by hand.
- `CrazyCraft5-Server.zip`: the dedicated server files. Unzip them, then double-click `CrazyCraft Server Setup.bat` (Windows) or run `./startserver.sh` (Linux and macOS).

---

## What's new: the First Night and Week (OreSpawn Integrations 0.10.1)
- **The Land's Week.** The Land keeps its own day count, and the sunrise names the weekday.
- **The Almanac.** One event a night. Thursday and Friday stay for the Land's own business.
- **The Hoard Fall.** A warning, a gold streak, three Aliens around a crater and a vault that scales with your rank. It digs no holes in your base.
- **The Mãe-da-lua.** One line at dusk, and the urutau spawns at last in the Old Ground and the Greenwood.
- **The Interregnum Gazette.** A newspaper every dawn in your own language, with yesterday's biggest deed and Today's Misfile.
- **The Deed Log.** Your deeds are kept and told one size bigger every Saturday.
- **The Hunt Lodge and the Wall of the Wanted.** Captain Iracema's home, seven Wanted posters and three bounties every Monday.
- **Seu Zé's boteco.** A bar you can find. On Saturday at dusk, everyone at the counter hears their own deeds retold.
- **Wednesday's peddler.** A trader and two llamas come to a newcomer's bed or the week's best-kept house.
- **The Mapinguari's Sunday.** Hunt the Land's animals away from home on a Sunday and something comes at dusk.
- **The Parish Stone and the Plot Stake.** A stone counts what you build near it, through four stages.
- **Court ranks.** Your rank is the same everywhere, and players from before this version are backfilled at their next login.

## Fixes since alpha.3
- **Shelf 9 turns up on more new worlds.** In our tests on 24 worlds, the old 48-block search found no site on 6 of them. It now reads ungenerated ground properly and keeps looking out to 192 blocks, and all 24 placed. It also no longer settles on a sky-ship's deck or inside another structure.
- **Every new world looks for its Hunt Lodge.** When the world's own placement finds nothing, usually out at sea, the first start puts the Lodge on good ground 300 to 600 blocks out. In our tests on 8 new worlds, all 8 got one. A world with no land in range still gets the Monday board at Monster Hunter tents and Registry desks.
- **A Royal Casket's trap no longer burns or breaks a roof.** Since alpha.1, a trap's fireball could set a roof alight and its TNT could blow holes in a roof and floor. Its blasts now hurt only the players nearby.
- Captain Iracema is no longer lost when her Lodge appears before she can: she waits until she can stand there, through a restart too.
- The Registry pin on a far village no longer sits at the bottom of the world on the map.
- The game no longer crashes in the creative inventory with Refurbished Furniture. If you installed the hotfix jar, this update removes it.
- Long region names and subtitles no longer run off the screen on small GUI scales.
- A fresh install opens straight to the title screen, without vanilla's first-run accessibility screen. The narrator choice stays in the options menu.
- Baron von Bats joins the other Plants & Zombies bosses: it counts as a heavyweight and can't be caged in a spawner.

## Pack changes
- OreSpawn Integrations 0.10.0 → 0.10.1.
- Removes `cc_hotfix_refurbished` if it's installed.

## Known issues
- A world with no fitting land within 192 blocks plays without Shelf 9. In testing none of the 24 worlds hit this, but it can happen.
- A world with no fitting land 300 to 600 blocks out has no Hunt Lodge; its Monday board is at the tents and desks.
- `minecraft:logs` includes Distant Lands' planks in this pack, so a Curupira can't break those planks, and a Beaver can eat them.

---

## Antes de jogar
- **Faça backup dos seus mundos primeiro.** Copie a pasta `saves/` (ou a pasta do mundo do servidor) para um lugar seguro.
- **Importe o zip do cliente como uma nova instância, ou deixe uma instância alfa se atualizar.** O instalador e a verificação de atualização do jogo só oferecem versões completas, então as instalações v1.4.x continuam na v1.4.x.
- **Não é para o hub v1.4.x.** Um cliente v2.0.0-alpha.4 não entra num servidor v1.4.x, nem o contrário.
- **O primeiro mundo pode demorar.** Criar o primeiro mundo pode levar até cerca de um minuto num portátil, enquanto a Prateleira 9 e o Pavilhão de Caça procuram onde ficar.

## Downloads
- `CrazyCraft5-Client.zip`: o pack do cliente. Importe no Prism Launcher ou no MultiMC (Adicionar Instância, depois Importar) e reserve de 8 a 12 GB de RAM. A primeira abertura baixa os mods das páginas oficiais. O MCHeli ainda precisa ser baixado à mão.
- `CrazyCraft5-Server.zip`: os arquivos do servidor dedicado. Descompacte e dê dois cliques em `CrazyCraft Server Setup.bat` (Windows) ou rode `./startserver.sh` (Linux e macOS).

---

## O que há de novo: a Primeira Noite e a Semana (OreSpawn Integrations 0.10.1)
- **A Semana da Terra.** A Terra conta os próprios dias, e o nascer do sol diz o dia da semana.
- **O Almanaque.** Um evento por noite. Quinta e sexta ficam para os assuntos da Terra.
- **A Queda do Tesouro.** Um aviso, um rastro dourado, três Alienígenas em volta de uma cratera e um cofre conforme o seu posto. Ela não abre buraco na sua base.
- **A Mãe-da-lua.** Uma fala ao anoitecer, e o urutau finalmente aparece no Chão Velho e na Mata.
- **O Diário Oficial do Interregno.** Um jornal a cada amanhecer na sua língua, com o maior feito de ontem e o Extravio do dia.
- **O livro de feitos.** Seus feitos ficam guardados e são contados um tamanho maior a cada sábado.
- **O Pavilhão de Caça e o Mural dos Procurados.** A casa da Capitã Iracema, sete cartazes de Procurado e três recompensas toda segunda-feira.
- **O boteco do Seu Zé.** Um bar que dá para achar. No sábado ao anoitecer, quem está no balcão ouve os próprios feitos recontados.
- **O mascate de quarta-feira.** Um mercador e duas lhamas vêm até a cama de um novato ou à casa mais bem cuidada da semana.
- **O Domingo do Mapinguari.** Cace os animais da Terra longe de casa num domingo e alguma coisa vem ao anoitecer.
- **O Marco da Freguesia e a Estaca de Lote.** Um marco conta o que você constrói perto dele, em quatro etapas.
- **Postos da corte.** Seu posto é o mesmo em todo lugar, e quem já jogava antes desta versão recebe o seu no próximo login.

## Correções desde a alpha.3
- **A Prateleira 9 aparece em mais mundos novos.** Nos nossos testes com 24 mundos, a busca antiga de 48 blocos não achou lugar em 6 deles. Agora ela lê direito o chão ainda não gerado e continua procurando até 192 blocos, e os 24 receberam a Prateleira. Ela também não pousa mais no convés de um navio voador nem dentro de outra estrutura.
- **Todo mundo novo procura o seu Pavilhão de Caça.** Quando o posicionamento do próprio mundo não acha nada, quase sempre no mar, a primeira abertura põe o Pavilhão em chão bom a 300–600 blocos. Nos nossos testes com 8 mundos novos, os 8 ganharam um. Um mundo sem terra ao alcance continua com o quadro de segunda-feira nas tendas de Caçador e nos Balcões do Registro.
- **A armadilha de uma Arca Real não queima nem quebra mais um telhado.** Desde a alpha.1, a bola de fogo de uma armadilha podia pôr fogo num telhado e o TNT podia abrir buracos no telhado e no chão. Agora as explosões dela só ferem os jogadores por perto.
- A Capitã Iracema não se perde mais quando o Pavilhão aparece antes dela: ela espera até poder ficar lá, inclusive depois de reiniciar.
- O marcador do Registro numa vila distante não fica mais no fundo do mundo no mapa.
- O jogo não fecha mais no inventário criativo com o Refurbished Furniture. Se você instalou o jar de correção, esta atualização o remove.
- Nomes e subtítulos longos de região não saem mais da tela em escalas pequenas.
- Uma instalação nova abre direto na tela de título, sem a tela de acessibilidade da primeira execução. A escolha do narrador continua no menu de opções.
- O Barão Von Morcego se junta aos outros chefes de Plants & Zombies: conta como peso-pesado e não pode ser preso num gerador.

## Mudanças no pack
- OreSpawn Integrations 0.10.0 → 0.10.1.
- Remove o `cc_hotfix_refurbished`, se estiver instalado.

## Problemas conhecidos
- Um mundo sem terra adequada a até 192 blocos fica sem a Prateleira 9. Nos testes, nenhum dos 24 mundos passou por isso, mas pode acontecer.
- Um mundo sem terra adequada a 300–600 blocos fica sem o Pavilhão de Caça; o quadro de segunda-feira fica nas tendas e nos balcões.
- `minecraft:logs` inclui as tábuas do Distant Lands neste pack, então o Curupira não quebra essas tábuas, e o Castor pode comê-las.
