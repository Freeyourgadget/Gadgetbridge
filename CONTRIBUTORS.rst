.. 2>/dev/null
 names () 
 { 
 echo -e "\n exit;\n**Contributors (sorted by number of commits):**\n";
 git log --format='%aN:%aE' origin/master | grep -Ev "(anonymous:|FYG_.*_bot_ignore_me|reformat-bot@freeyourgadget.org)" | sed 's/@users.github.com/@users.noreply.github.com/g' | awk 'BEGIN{FS=":"}{ct[$1]+=1;e[$1]=$2}END{for (i in e)  { n[i]=e[i];c[i]+=ct[i] }; for (a in e) print c[a]"\t* "a" <"n[a]">";}' | sort -n -r | cut -f 2-
 }
 quine () 
 { 
 { 
 echo ".. 2>/dev/null";
 declare -f names | sed -e 's/^[[:space:]]*/ /';
 declare -f quine | sed -e 's/^[[:space:]]*/ /';
 echo -e " quine\n";
 names;
 echo -e "\nAnd all the former Transifex translators, who cannot be listed automatically.\n\n*To update the contributors list just run this file with bash. Prefix a name with % in .mailmap to set a contact as preferred*"
 } > CONTRIBUTORS.rst;
 exit
 }
 quine


 exit;
**Contributors (sorted by number of commits):**

* Andreas Shimokawa <shimokawa@fsfe.org>
* Carsten Pfeiffer <cpfeiffer@users.noreply.github.com>
* José Rebelo <joserebelo@outlook.com>
* Daniele Gobbetti <daniele+github@gobbetti.name>
* Petr Vaněk <petr@linuks.cz>
* Yaron Shahrabani <sh.yaron@gmail.com>
* Daniel Dakhno <dakhnod@gmail.com>
* 陈少举 <oshirisu.red@gmail.com>
* Arjan Schrijver <arjan5@noreply.codeberg.org>
* Vincèn PUJOL <vincen@vincen.org>
* Oğuz Ersen <oguzersen@protonmail.com>
* Allan Nordhøy <epost@anotheragency.no>
* Ihor Hordiichuk <igor_ck@outlook.com>
* nautilusx <mail.ka@mailbox.org>
* Taavi Eomäe <taavi.eomae+github@gmail.com>
* Gordon Williams <gw@pur3.co.uk>
* Rafael Fontenelle <rafaelff@gnome.org>
* Michal L <michalrmsmi@wp.pl>
* Sebastian Kranz <lightwars@web.de>
* João Paulo Barraca <jpbarraca@gmail.com>
* Linerly <linerly@protonmail.com>
* Rex_sa <rex.sa@pm.me>
* mamucho <mamut_killer@yahoo.com>
* postsorino <postsorino@krutt.org>
* Manuel-Senpai <senpai99@hotmail.com>
* Andreas Böhler <dev@aboehler.at>
* FransM <fransmeulenbroeks@yahoo.com>
* Jonas <jonasdcdm@posteo.net>
* HenRy <helge1o1o1@gmail.com>
* Yukai Li <yukaili.geek@gmail.com>
* Roi Greenberg <roigreenberg@gmail.com>
* gallegonovato <fran-carro@hotmail.es>
* Nikita Epifanov <nikgreens@protonmail.com>
* kirill blaze <kirillblaze2@gmail.com>
* Óscar Fernández Díaz <oscfdezdz@tuta.io>
* Jeannette L <j.lavoie@net-c.ca>
* Vadim Kaushan <admin@disasm.info>
* protomors <protomors@gmail.com>
* Cre3per <lukas96s@web.de>
* Davis Mosenkovs <davikovs@gmail.com>
* ssantos <ssantos@web.de>
* Michael <quelbs@gmail.com>
* glemco <glemco@codeberg>
* 115ek <e.blosz@hotmail.de>
* 0que <0que@users.noreply.hosted.weblate.org>
* Саша Петровић <salepetronije@gmail.com>
* naofum <naofum@gmail.com>
* My Random Thoughts <weblate@myrandomthoughts.co.uk>
* Damien 'Psolyca' Gaignon <damien.gaignon@gmail.com>
* 0eoc <0eoc@users.noreply.hosted.weblate.org>
* mesnevi <shams@airpost.net>
* Kintu <kintukp@gmail.com>
* youzhiran <2668760098@qq.com>
* mueller-ma <mueller-ma@users.noreply.github.com>
* ivanovlev <ivanovlev@mail.ru>
* Tijl Schepens <tijl.schepens@hotmail.com>
* Sophanimus <sennamails@googlemail.com>
* Pavel Elagin <elagin.pasha@gmail.com>
* NekoBox <nekobox@noreply.codeberg.org>
* MPeter <>
* MrYoranimo <yvulker@gmail.com>
* mondstern <hello@mondstern.tk>
* Hadrián Candela <hadrian.candela@gmail.com>
* Ács Zoltán <acszoltan111@gmail.com>
* Zhong Jianxin <azuwis@gmail.com>
* Milo Ivir <mail@milotype.de>
* Gabriele Monaco <monaco@eit.uni-kl.de>
* foxstidious <foxstidious@gmail.com>
* Andy Yang <a962702@yahoo.com>
* Abdullah Manaz <manaz@noreply.codeberg.org>
* Richard de Boer <git@tubul.net>
* mkusnierz <>
* Julien Pivotto <roidelapluie@inuits.eu>
* tomechio <tomasz@salamon.fi>
* Steffen Liebergeld <perl@gmx.org>
* Skrripy <rozihrash.ya6w7@simplelogin.com>
* Petr Kadlec <mormegil@centrum.cz>
* Pavel <pavel.gorbanj@gmail.com>
* Lem Dulfo <lemuel.dulfo@gmail.com>
* Dmitriy Bogdanov <di72nn@gmail.com>
* Olexandr Nesterenko <olexn@ukr.net>
* Nevena Mircheva <nevena.mircheva@gmail.com>
* musover <meoberto@mthree.es>
* Matthieu Baerts <matttbe@gmail.com>
* Felix Konstantin Maurer <maufl@maufl.de>
* Axus Wizix <aw.ts@bk.ru>
* Xtremo3 <a.lewicki95@gmail.com>
* Utsob Roy <uroybd@gmail.com>
* taras3333 <taras3333@gmail.com>
* Sergey Trofimov <sarg@sarg.org.ru>
* Sebastian Krey <sebastian@skrey.net>
* Noodlez <contact@nathanielbarragan.xyz>
* M. Hadi <hhhadddi@yahoo.com>
* Martin Boonk <martin@boonk.info>
* Lukas <lukas.edi@gmx.net>
* Ganblejs <ganblejs@noreply.codeberg.org>
* Deixondit <jperals@protonmail.com>
* akasaka / Genjitsu Labs <vladkorotnev@gmail.com>
* Szylu <chipolade@gmail.com>
* Robert Barat <rbarat07@gmail.com>
* Reza Almanda <rezaalmanda27@gmail.com>
* Mario <mariomobla@gmail.com>
* ksiwczynski <k.siwczynski@gmail.com>
* JohnnySun <bmy001@gmail.com>
* Gilles Émilien MOREL <contact@gilles-morel.fr>
* firekonstantin <firekonstantin@mail.ru>
* bruh <quangtrung02hn16@gmail.com>
* Uwe Hermann <uwe@hermann-uwe.de>
* Patric Gruber <me@patric-gruber.at>
* opavlov <forpoststuff@gmail.com>
* Michalis <michalisntovas@yahoo.gr>
* Mario Rossi <kk1o2n+61euckrwqwqecz3pme3@sharklasers.com>
* ifurther <i.further.5.4@gmail.com>
* Edoardo Rosa <edoardo.rosa90@gmail.com>
* d <dmanye@gmail.com>
* Bożydar <trening302@o2.pl>
* Alberto <albertsal83@gmail.com>
* AiLab <vpuhoff92@gmail.com>
* zsolt3991 <zsolt_93@yahoo.com>
* winver <kirillstuzhuk@gmail.com>
* Vladislav Serkov <vladserkoff@protonmail.com>
* Vebryn <vebryn@gmail.com>
* uli <cybuzuma@vnxs.de>
* Ted Stein <me@tedstein.net>
* sinore <sinoren263@niekie.com>
* Shimon <simonfarm0@gmail.com>
* Reiner Herrmann <reiner@reiner-h.de>
* NicoBuntu <nicolas__du95@hotmail.fr>
* Nee Sorry <sven.fasterding@posteo.de>
* Marc Nause <marc.nause@audioattack.de>
* Louis-Marie Croisez <louis.croisez@gmail.com>
* Kryštof Černý <cleverline1mc@gmail.com>
* Johannes Krude <johannes@krude.de>
* Jean-François Greffier <jf.greffier@gmail.com>
* Hasan Ammar <ammarh@gmail.com>
* Giuseppe Caliendo <giuseppe.caliendo@gmail.com>
* Gergely Peidl <gergely@peidl.net>
* Fabio Parri <parrif_ibb@yahoo.com>
* Evo <weblate@verahawk.com>
* Emre <wenigerpluesch@mailbox.org>
* Elwood <elwood21@gmail.com>
* Dmitry Markin <dmitry@markin.tech>
* CE4 <chregger@gmail.com>
* ce4 <ce4@posteo.de>
* Baka Gaijin <lewdwarrior@waifu.club>
* AndrewBedscastle <1462953+AndrewBedscastle@users.noreply.github.com>
* abettenburg <a.bettenburg@gmail.com>
* 0nse <0nse@users.noreply.github.com>
* Максим Якимчук <xpinovo@gmail.com>
* Ye Wint Htut Kyaw <oxygen.2521998@gmail.com>
* xaos <xaos@noreply.codeberg.org>
* Thomas <tutonis@gmail.com>
* TheScientistPT <joao.ed.reis.gomes@gmail.com>
* SnowCat <kittythesnowcat@protonmail.com>
* Sergio Varela <sergitroll9@gmail.com>
* Sebastian Obrusiewicz <sobrus2@o2.pl>
* Sebastian Espinosa <hipypuff@gmail.com>
* Robbert Gurdeep Singh <git@beardhatcode.be>
* Rimas Raguliūnas <rarimas@gmail.com>
* mvn23 <schopdiedwaas@gmail.com>
* Minori Hiraoka (미노리) <minori@mnetwork.co.kr>
* MASVA <masva10@gmail.com>
* masakoodaa <masakoodaa@protonmail.com>
* Marius Cornescu <marius_cornescu@yahoo.com>
* mantas-p <megaliuz@gmail.com>
* Lukas Veneziano <fs@venezilu.de>
* LL <lu.lecocq@free.fr>
* LizardWithHat <martin.betcher@gmail.com>
* Lesur Frederic <contact@memiks.fr>
* leela <53352@protonmail.com>
* kukuruzka <anton.dan1657@gmail.com>
* Kompact <joaorafael123@hotmail.com>
* Kalle <hiwokey596@vip4e.com>
* K0L0B0G <github@gorobav.ru>
* Johann C. Rode <jcrode@ece.ucsb.edu>
* jimman2003 <jim41825@gmail.com>
* jfgreffier <jfgreffier@users.noreply.github.com>
* Jasper <jespiex456@hotmail.com>
* ITCactus <itcactus@noreply.codeberg.org>
* illis <illis@beyond8labs.com>
* Francesco Marinucci <francesco.marinucci@posteo.net>
* FintasticMan <finlay.neon.kid@gmail.com>
* Doma Gergő <domag02@gmail.com>
* Dikay900 <dark900@xyz.de>
* Denis <korden@sky-play.ru>
* Christian Fischer <sw-dev@computerlyrik.de>
* Benjamin Swartley <reep236@gmail.com>
* Asbesbopispa <c.alfano+github@outlook.it>
* Adolfo Jayme Barrientos <fitojb@ubuntu.com>
* 6arms1leg <m.brnsfld@googlemail.com>
* Your Name <you@example.com>
* XqweX <xqwex@seznam.cz>
* walkjivefly <mark@walkjivefly.com>
* WaldiS <admin@sto.ugu.pl>
* Vytenis <slivinskasvytenis@gmail.com>
* Vladislav Glinsky <cl0ne@mithril.org.ua>
* vishnu <vishnu012@protonmail.com>
* Vianney le Clément de Saint-Marcq <vianney@noreply.codeberg.org>
* Toby Murray <toby.murray+gitkraken@protonmail.com>
* thyttan <6uuxstm66@mozmail.com>
* Thorsten <js1_531b3pm29ng2@byom.de>
* Stephan Lachnit <stephanlachnit@protonmail.com>
* Sebastian Reichel <sre@ring0.de>
* Saul Nunez <saul.nunez99@gmail.com>
* Rui Mendes <xz9@protonmail.com>
* roolx <rbzikadze@gmail.com>
* rarder44 <rarder44@noreply.codeberg.org>
* rany <ranygh@riseup.net>
* Ranved Sticon <the7bulk@gmail.com>
* Rajesh Kumbhakar <sssraj.sssraj@gmail.com>
* Ptilopsis Leucotis <PtilopsisLeucotis@yandex.com>
* petronovak <petro.novak@gmail.com>
* Pascal <pascal.tannich@gmail.com>
* odavo32nof <odavo32nof@noreply.codeberg.org>
* octospacc <octo@tutamail.com>
* NotAFIle <nota@notafile.com>
* Normano64 <per.bergqwist@gmail.com>
* Nikolay Korotkiy <sikmir@gmail.com>
* Nick Spacek <peter.spacek@siemens.com>
* Nekromanser <ari.taitto@protonmail.com>
* Nathan <bonnemainsnathan@gmail.com>
* narektor <narektor@noreply.codeberg.org>
* MyTimeKill <26295589+MyTimeKill@users.noreply.github.com>
* Molnár Barnabás <nsd4rkn3ss@gmail.com>
* Moarc <aldwulf@gmail.com>
* Michal Novotny <mignov@gmail.com>
* maxvel <maxvel@noreply.codeberg.org>
* Maxime Reyrolle <dev@reyrolle.fr>
* Mattias Münster <mattiasmun@gmail.com>
* Mattherix <mattherix@protonmail.com>
* Martin <ritualz@users.noreply.github.com>
* marco.altomonte <marco.altomonte@gmail.com>
* Le Poisson Libre <services@spika.xyz>
* Krzysztof Marcinek <krzymar2002@gmail.com>
* krzys_h <krzys_h@interia.pl>
* Konrad Iturbe <KonradIT@users.noreply.github.com>
* Kamalei Zestri <38802353+KamaleiZestri@users.noreply.github.com>
* Joel Beckmeyer <joel@beckmeyer.us>
* Jesús <zaagur@gmail.com>
* Jesús F <jfmiguel@wanadoo.es>
* Irul <wedesignthing@gmail.com>
* Igor Polyakov <igorpolyakov@protonmail.com>
* homocomputeris <homocomputeris+git@gmail.com>
* Grzegorz <grzesjam@duck.com>
* GeekosaurusR3x <skad@protonmail.com>
* Francesco Franchina <cescus92@gmail.com>
* fparri <fparri@noreply.codeberg.org>
* Fabien Brachere <fabien@brachere.net>
* exit-failure <hakrala@web.de>
* Ertu (Er2, Err) <er2official@outlook.com>
* Er2 <er2@dismail.de>
* Dreamwalker <aristojeff@gmail.com>
* DAWID <aggregate_diffused400@simplelogin.com>
* Dario Lopez-Kästen <cl2dlope@gmail.com>
* Da Pa <da.pa@noreply.codeberg.org>
* DanialHanif <danialhanif@outlook.com>
* Cristian Alfano <c.alfano@outlook.it>
* criogenic <criogenic@gmail.com>
* chabotsi <chabotsi+github@chabotsi.fr>
* bowornsin <bowornsin@gmail.com>
* Avamander <Avamander@users.noreply.github.com>
* Artem <KovalevArtem.ru@gmail.com>
* AnthonyDiGirolamo <anthony.digirolamo@gmail.com>
* Anonymous <noreply@weblate.org>
* Andreas Kromke <Andreas.Kromke@web.de>
* Alex <leha-bot@yandex.ru>
* Albert <albert@avdm043>
* Ainārs <ainars71@inbox.lv>
* عبدالرئوف عابدی <abdolraoofabedi@gmail.com>
* Егор Ермаков <eg.ermakov2016@yandex.ru>
* Ⲇⲁⲛⲓ Φi <daniphii@outlook.com>
* Yusuf Cihan <yusufcihandemirbas@gmail.com>
* yk <bivol21883@cyadp.com>
* Yar <yaroslav.isakov@gmail.com>
* xzovy <caleb@caleb-cooper.net>
* xphnx <xphnx@users.noreply.github.com>
* Xosé M <xosem@disroot.org>
* Xeoy <arventh@pm.me>
* Xavier RENE-CORAIL <xavier.renecorail@gmail.com>
* x29a <x29a@noreply.codeberg.org>
* w2q <w2q@noreply.codeberg.org>
* Vitaliy Shuruta <vshuruta@gmail.com>
* veecue <veecue@ventos.tk>
* Unixware <csynt@hotmail.com>
* TylerWilliamson <tyler.williamson51@gmail.com>
* Triet Pham <triet.phm@gmail.com>
* Traladarer <Traladarer@users.noreply.hosted.weblate.org>
* Tomer Rosenfeld <tomerosenfeld007@gmail.com>
* Tomas Radej <tradej@redhat.com>
* t-m-w <t-m-w-codeberg@galac.tk>
* tiparega <11555126+tiparega@users.noreply.github.com>
* TinfoilSubmarine <tinfoilsubmarine@noreply.codeberg.org>
* Tim <tim.w1995@gmail.com>
* thirschbuechler <thirschbuechler@github.com>
* Thiago Rodrigues <thiaguinho.the@gmail.com>
* thermatk <thermatk@noreply.codeberg.org>
* theghostofheathledger <theghostofheathledger@noreply.codeberg.org>
* Temuri Doghonadze <temuri.doghonadze@gmail.com>
* Tarik Sekmen <tarik@ilixi.org>
* Szymon Tomasz Stefanek <s.stefanek@gmail.com>
* szilardx <15869670+szilardx@users.noreply.github.com>
* Swann Martinet <swann.ranskassa@laposte.net>
* Stefan Bora <stefan.bora@outlook.de>
* Stan Gomin <stan@gomin.me>
* ssilverr <ssilverr@noreply.codeberg.org>
* Sky233ml <sky233@sky233.ml>
* SinMan <emilio.galvan@gmail.com>
* Simon Sievert <ssievert@protonmail.com>
* Sergio Lopez <slp@sinrega.org>
* Sergey Vasilyev <koniponi@gmail.com>
* sedy89 <sedy89@noreply.codeberg.org>
* Sebastian Nilsson <sebbe_kompis@hotmail.com>
* S Dantas <dantasosteney@gmail.com>
* Santiago Benalcázar <santiagosdbc@gmail.com>
* Samuel Carvalho de Araújo <samuelnegro12345@gmail.com>
* Sami Alaoui <4ndroidgeek@gmail.com>
* Saman rsh <sasan.r75@gmail.com>
* Salif Mehmed <mail@salif.eu>
* SalavatR <salavat.rakhmanov@gmail.com>
* Roxystar <roxystar@arcor.de>
* Roman Plevka <rplevka@redhat.com>
* rom4nik <codeberg@rom4nik.pl>
* Robin Davidsson <robindavidsson@outlook.com>
* Roberto P. Rubio <outtakes_handgun090@familiapm.com>
* rober <rober@prtl.nodomain.net>
* Rivo Zängov <rivozangov@gmail.com>
* rimasx <riks_12@hot.ee>
* rikka356 <rikka356@outlook.com>
* Richard Finegold <goldfndr+git@gmail.com>
* Retew <salau@tutanota.com>
* redking <redking974@gmail.com>
* Ray <RayL-er@outlook.com>
* RandomItalianGuy <donatoperruso@protonmail.com>
* Raghd Hamzeh <raghd@rhamzeh.com>
* Quang Ngô <ngoquang2708@gmail.com>
* Quallenauge <Hamsi2k@freenet.de>
* Q-er <9142398+Q-er@users.noreply.github.com>
* pommes <pommes@noreply.codeberg.org>
* pishite <pishite@meta.ua>
* Perflyst <mail@perflyst.de>
* Pavel Motyrev <legioner.r@gmail.com>
* Pauli Salmenrinne <susundberg@gmail.com>
* pangwalla <pangwalla@noreply.codeberg.org>
* Pander <pander@users.sourceforge.net>
* ozkanpakdil <ozkan.pakdil@gmail.com>
* opcode <luigi@sciolla.net>
* Ondřej Sedláček <ond.sedlacek@gmail.com>
* Olivier Bloch <blochjunior@gmail.com>
* Oleg Vasilev <oleg.vasilev@virtuozzo.com>
* Oleg <oleg.invisibleman@protonmail.com>
* Nur Aiman Fadel <nuraiman@gmail.com>
* Nikolai Sinyov <nikolay.sinyov@yandex.ru>
* Nicolò Balzarotti <anothersms@gmail.com>
* Nephiel <Nephiel@users.noreply.github.com>
* Nathan Philipp Bo Seddig <natpbs@gmail.com>
* Natanael Arndt <arndtn@gmail.com>
* Nabil BENDAFI <nabil@bendafi.fr>
* myxor <myxor@noreply.codeberg.org>
* Morten Rieger Hannemose <mohan@dtu.dk>
* Mirko Covizzi <mrkcvzz@gmail.com>
* Milan Šalka <salka.milan@googlemail.com>
* Mike van Rossum <mike@vanrossum.net>
* mika laka <Mikhaila.Eaddy@easymailer.live>
* Michal Novak <michal.novak@post.cz>
* Michael Wiesinger <michw2014@gmail.com>
* michaelneu <git@michaeln.eu>
* MedusasSphinx <medusassphinx@noreply.codeberg.org>
* McSym28 <McSym28@users.noreply.github.com>
* MaxL <z60loa8qw3umzu3@my10minutemail.com>
* maxirnilian <maxirnilian@users.noreply.github.com>
* Maxim Baz <git@maximbaz.com>
* Mave95 <mave95@noreply.codeberg.org>
* Matej Drobnič <matejdro@gmail.com>
* Marvin D <mave95@posteo.de>
* Martin Piatka <chachacha2323@gmail.com>
* Martin.JM <>
* Margreet <margreetkeelan@gmail.com>
* Marc Schlaich <marc.schlaich@googlemail.com>
* Marco Alberto Diosdado Nava <betoxxdiosnava@gmail.com>
* Marco A <35718078+TomasCartman@users.noreply.github.com>
* Marc Laporte <marc@laporte.name>
* Marcin <ml.cichy@gmail.com>
* Marcel pl (m4rcel) <marcel.garbarczyk@gmail.com>
* Manuel Soler <vg8020@gmail.com>
* Manuel Ruß <manuel_russ@dismail.de>
* mangel <mangelcursos@gmail.com>
* magimel.francois <magimel.francois@gmail.com>
* Maciej Kuśnierz <>
* m4sk1n <me@m4sk.in>
* LukasEdl <lukasedl@noreply.codeberg.org>
* LuK1337 <priv.luk@gmail.com>
* Luiz Felipe das Neves Lopes <androidfelipe23@gmail.com>
* Luis zas <dalues@gmail.com>
* Ludovic Jozeau <unifai@protonmail.com>
* luca sain <luca.sain@outlook.com>
* lucanomax <lucano.valo@gmail.com>
* Liao junchao <liaojunchao@outlook.com>
* Leon Omelan <rozpierog@gmail.com>
* Leonardo Amaral <contato@leonardoamaral.com.br>
* Leo bonilla <leo_lf9@hotmail.com>
* LeJun <lejun@gmx.fr>
* Lejun <adrienzhang@hotmail.com>
* lazarosfs <lazarosfs@csd.auth.gr>
* Lars Vogdt <lars.vogdt@suse.com>
* ladbsoft <30509719+ladbsoft@users.noreply.github.com>
* Kyaw Min Khant <kyawmink@gmail.com>
* Krisztián Gáncs <990024@gmail.com>
* Kristjan Räts <kristjanrats@gmail.com>
* Kornél Schmidt <kornel.schmidt@clubspot.app>
* kirk1984 <kirk1984@noreply.codeberg.org>
* kieranc001 <kieranc001@noreply.codeberg.org>
* kevlarcade <kevlarcade@gmail.com>
* Kevin Richter <me@kevinrichter.nl>
* Kevin MacMartin <prurigro@gmail.com>
* keeshii <keeshii@ptcg.eu>
* Kaz Wolfe <root@kazwolfe.io>
* Kasha <kasha_malaga@hotmail.com>
* kalaee <alex.kalaee@gmail.com>
* Julien Winning <heijulien@web.de>
* Julian Lam <julian@nodebb.org>
* jugendhacker <jugendhacker@gmail.com>
* Joseph Kim <official.jkim@gmail.com>
* jonnsoft <>
* Johannes Tysiak <vinyl@users.sf.net>
* Jochen S <tsalin@noreply.codeberg.org>
* joaquim.org <joaquim.org@gmail.com>
* jhey <jhey@noreply.codeberg.org>
* JF <jf@codingfield.com>
* Jean-François Milants <jf@codingfield.com>
* jcrode <46062294+jcrode@users.noreply.github.com>
* Jan Lolek <janlolek@seznam.cz>
* Jakub Jelínek <jakub.jelinek@gmail.com>
* Jacque Fresco <aidter@use.startmail.com>
* Izzy <izzy@qumran.org>
* iwonder <hosted.weblate.org@heychris.eu>
* Ivan <ivan_tizhanin@mail.ru>
* InternalErrorX <internalerrorx@noreply.codeberg.org>
* Hüseyin Aslan <ha098784@gmail.com>
* Hugel <qihu@nfschina.com>
* hr-sales <hericsonregis@hotmail.com>
* Hirnchirurg <anonymous11@posteo.net>
* Hen Ry <nobo@go4more.de>
* HelloCodeberg <hellocodeberg@noreply.codeberg.org>
* HardLight <hardlightxda@gmail.com>
* Hanhan Husna <matcherapy@gmail.com>
* halemmerich <halemmerich@noreply.codeberg.org>
* hackoder <hackoder@noreply.codeberg.org>
* Gustavo Ramires <gustavo.nramires@gmail.com>
* gsbhat <>
* Grzegorz Dznsk <grantmlody96@gmail.com>
* Golbinex <2061409-Golbinex@users.noreply.gitlab.com>
* gnufella <gnufella@noreply.codeberg.org>
* gnu-ewm <gnu.ewm@protonmail.com>
* Gleb Chekushin <mail@glebchek.com>
* Giuseppe <giuseppe.parasilitipalumbo@studium.unict.it>
* Gideão Gomes Ferreira <trjctr@gmail.com>
* gfwilliams <gfwilliams@noreply.codeberg.org>
* GabO <gabosuelto@gmail.com>
* Gabe Schrecker <gabe@pbrb.co.uk>
* freezed-or-frozen <freezed.or.frozen@gmail.com>
* Frank Slezak <KazWolfe@users.noreply.github.com>
* Frank Ertl <hrglpfrmpf@noreply.codeberg.org>
* Florian Beuscher <florianbeuscher@gmail.com>
* Fabian Hof <weblate@fabian-hof.de>
* Étienne Deparis <etienne@depar.is>
* Estébastien Robespi <estebastien@mailbox.org>
* Ernst <ernst@seebens.de>
* Enrico Brambilla <enricobilla@noreply.codeberg.org>
* Edoardo Tronconi <edoardo.tronconi@gmail.com>
* Dougal19 <4662351+Dougal19@users.noreply.github.com>
* Donato <pread.xa4mx@simplelogin.com>
* Dmytro Bielik <mitrandir.hex@gmail.com>
* djurik <dirceu.semighini@protonmail.com>
* DerFetzer <kontakt@der-fetzer.de>
* Dean <3114661520@qq.com>
* Deactivated Account <diastasis@gmail.com>
* David Girón <contacto@duhowpi.net>
* Davide Corradini <updates+weblate.org@corradinidavi.de>
* Daniel Thompson <daniel@redfelineninja.org.uk>
* Daniel Hauck <maill@dhauck.eu>
* Dam BOND <dambond2001@gmail.com>
* 이정희 <daemul72@gmail.com>
* Dachi G <duchy007@yahoo.com>
* C <weblate@wolki.de>
* cokecodecock <lights1140977891@163.com>
* CodeSpoof <nao.s_l_t_e_e_l@protonmail.com>
* C O <cosmin.oprisan@gmail.com>
* clach04 <Chris.Clark@actian.com>
* Chris Perelstein <chris.perelstein@gmail.com>
* chklump <chklump@noreply.codeberg.org>
* Cédric Bellegarde <cedric.bellegarde@adishatz.org>
* Carlos Ferreira <calbertoferreira@gmail.com>
* C0rn3j <spleefer90@gmail.com>
* ButterflyOfFire <ButterflyOfFire@protonmail.com>
* bucala <marcel.bucala@gmail.com>
* boun <boun@gmx.de>
* BobIsMyManager <bobismymanager@noreply.codeberg.org>
* Bilel MEDIMEGH <bilel.medimegh@gmail.com>
* Benjamin Kahlau <nyhkkbjyek@roanapur.de>
* Ben <ben.david.wallner@gmail.com>
* beardhatcode <beardhatcode@noreply.codeberg.org>
* batataspt@gmail.com <batataspt@gmail.com>
* atkyritsis <at.kyritsis@gmail.com>
* Ascense <ascense@noreply.codeberg.org>
* Aprilhoomie <Aprilhoomie@gmail.com>
* apre <adrienpre+github@gmail.com>
* Ann Test <testkimochiaz@gmail.com>
* Aniruddha Adhikary <aniruddha@adhikary.net>
* angelpup <angelpup@noreply.codeberg.org>
* Anemograph <dyraybn@gmail.com>
* Andrzej Surowiec <emeryth@gmail.com>
* Andrew Watkins <randnv20@noreply.codeberg.org>
* andrewlytvyn <indusfreelancer@gmail.com>
* AndrewH <36428679+andrewheadricke@users.noreply.github.com>
* andre <andre.buesgen@yahoo.de>
* Andrea Lepori <mafaldo@hotmail.it>
* Allen B <28495335+Allen-B1@users.noreply.github.com>
* Alicia Hormann <ahormann@gmx.net>
* Alfeu Lucas Guedes dos Santos <alfeugds@gmail.com>
* Alexey Afanasev <avafanasiev@gmail.com>
* Alexandra Sevostyanova <asevostyanova@gmail.com>
* Aidan Crane <aidancrane78@gmail.com>
* aerowolf <aerowolf@tom.com>
* Adam Büchner <buechner.adam@gmx.de>
* a b <65567823+abb128@users.noreply.github.com>

And all the former Transifex translators, who cannot be listed automatically.

*To update the contributors list just run this file with bash. Prefix a name with % in .mailmap to set a contact as preferred*
