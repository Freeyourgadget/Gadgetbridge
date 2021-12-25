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
* Daniele Gobbetti <daniele+github@gobbetti.name>
* Daniel Dakhno <dakhnod@gmail.com>
* Petr Vaněk <petr@linuks.cz>
* Yaron Shahrabani <sh.yaron@gmail.com>
* Allan Nordhøy <epost@anotheragency.no>
* Taavi Eomäe <taavi.eomae+github@gmail.com>
* 陈少举 <oshirisu.red@gmail.com>
* Rafael Fontenelle <rafaelff@gnome.org>
* João Paulo Barraca <jpbarraca@gmail.com>
* Sebastian Kranz <tklightforce@googlemail.com>
* nautilusx <mail.ka@mailbox.org>
* mamucho <mamut_killer@yahoo.com>
* postsorino <postsorino@krutt.org>
* Oğuz Ersen <oguzersen@protonmail.com>
* FransM <fransmeulenbroeks@yahoo.com>
* Andreas Böhler <dev@aboehler.at>
* Jonas <jonasdcdm@posteo.net>
* Yukai Li <yukaili.geek@gmail.com>
* Roi Greenberg <roigreenberg@gmail.com>
* HenRy <helge1o1o1@gmail.com>
* Vadim Kaushan <admin@disasm.info>
* protomors <protomors@gmail.com>
* Cre3per <lukas96s@web.de>
* Michal L <michalrmsmi@wp.pl>
* José Rebelo <joserebelo@outlook.com>
* Vincèn PUJOL <vincen@vincen.org>
* Nikita Epifanov <nikgreens@protonmail.com>
* Michael <quelbs@gmail.com>
* 115ek <e.blosz@hotmail.de>
* naofum <naofum@gmail.com>
* Gordon Williams <gw@pur3.co.uk>
* mesnevi <shams@airpost.net>
* Jeannette L <j.lavoie@net-c.ca>
* youzhiran <2668760098@qq.com>
* mueller-ma <mueller-ma@users.noreply.github.com>
* ivanovlev <ivanovlev@mail.ru>
* Tijl Schepens <tijl.schepens@hotmail.com>
* ssantos <ssantos@web.de>
* Sophanimus <sennamails@googlemail.com>
* Pavel Elagin <elagin.pasha@gmail.com>
* mondstern <hello@mondstern.tk>
* Hadrián Candela <hadrian.candela@gmail.com>
* Zhong Jianxin <azuwis@gmail.com>
* Kintu <kintukp@gmail.com>
* Abdullah Manaz <manaz@noreply.codeberg.org>
* mkusnierz <>
* Julien Pivotto <roidelapluie@inuits.eu>
* Steffen Liebergeld <perl@gmx.org>
* Lem Dulfo <lemuel.dulfo@gmail.com>
* Nevena Mircheva <nevena.mircheva@gmail.com>
* Matthieu Baerts <matttbe@gmail.com>
* J. Lavoie <j.lavoie@net-c.ca>
* Felix Konstantin Maurer <maufl@maufl.de>
* Andy Yang <a962702@yahoo.com>
* Utsob Roy <uroybd@gmail.com>
* taras3333 <taras3333@gmail.com>
* Sergey Trofimov <sarg@sarg.org.ru>
* M. Hadi <hhhadddi@yahoo.com>
* Szylu <chipolade@gmail.com>
* Robert Barat <rbarat07@gmail.com>
* Pavel <pavel.gorbanj@gmail.com>
* Mario <mariomobla@gmail.com>
* ksiwczynski <k.siwczynski@gmail.com>
* JohnnySun <bmy001@gmail.com>
* Gilles Émilien MOREL <contact@gilles-morel.fr>
* Deixondit <jperals@protonmail.com>
* Uwe Hermann <uwe@hermann-uwe.de>
* opavlov <forpoststuff@gmail.com>
* Olexandr Nesterenko <olexn@ukr.net>
* Edoardo Rosa <edoardo.rosa90@gmail.com>
* Dmitriy Bogdanov <di72nn@gmail.com>
* Bożydar <trening302@o2.pl>
* Alberto <albertsal83@gmail.com>
* zsolt3991 <zsolt_93@yahoo.com>
* Vladislav Serkov <vladserkoff@protonmail.com>
* Vebryn <vebryn@gmail.com>
* Ted Stein <me@tedstein.net>
* NicoBuntu <nicolas__du95@hotmail.fr>
* Louis-Marie Croisez <louis.croisez@gmail.com>
* Jean-François Greffier <jf.greffier@gmail.com>
* Giuseppe Caliendo <giuseppe.caliendo@gmail.com>
* Gergely Peidl <gergely@peidl.net>
* Fabio Parri <parrif_ibb@yahoo.com>
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
* SnowCat <kittythesnowcat@protonmail.com>
* Sebastian Obrusiewicz <sobrus2@o2.pl>
* Rimas Raguliūnas <rarimas@gmail.com>
* Minori Hiraoka (미노리) <minori@mnetwork.co.kr>
* masakoodaa <masakoodaa@protonmail.com>
* Marius Cornescu <marius_cornescu@yahoo.com>
* Mario Rossi <kk1o2n+61euckrwqwqecz3pme3@sharklasers.com>
* Lukas Veneziano <fs@venezilu.de>
* LL <lu.lecocq@free.fr>
* leela <53352@protonmail.com>
* Kompact <joaorafael123@hotmail.com>
* K0L0B0G <github@gorobav.ru>
* Johann C. Rode <jcrode@ece.ucsb.edu>
* jfgreffier <jfgreffier@users.noreply.github.com>
* Jasper <jespiex456@hotmail.com>
* Francesco Marinucci <francesco.marinucci@posteo.net>
* Dikay900 <dark900@xyz.de>
* Denis <korden@sky-play.ru>
* Christian Fischer <sw-dev@computerlyrik.de>
* Asbesbopispa <c.alfano+github@outlook.it>
* AiLab <vpuhoff92@gmail.com>
* Adolfo Jayme Barrientos <fitojb@ubuntu.com>
* 6arms1leg <m.brnsfld@googlemail.com>
* XqweX <xqwex@seznam.cz>
* walkjivefly <mark@walkjivefly.com>
* WaldiS <admin@sto.ugu.pl>
* Vytenis <slivinskasvytenis@gmail.com>
* Vladislav Glinsky <cl0ne@mithril.org.ua>
* vishnu <vishnu012@protonmail.com>
* Thomas <tutonis@gmail.com>
* Sebastian Espinosa <hipypuff@gmail.com>
* Saul Nunez <saul.nunez99@gmail.com>
* Rui Mendes <xz9@protonmail.com>
* Ranved Sticon <the7bulk@gmail.com>
* Rajesh Kumbhakar <sssraj.sssraj@gmail.com>
* petronovak <petro.novak@gmail.com>
* Petr Kadlec <mormegil@centrum.cz>
* Pascal <pascal.tannich@gmail.com>
* odavo32nof <odavo32nof@noreply.codeberg.org>
* NotAFIle <nota@notafile.com>
* Normano64 <per.bergqwist@gmail.com>
* Nick Spacek <peter.spacek@siemens.com>
* Nee Sorry <sven.fasterding@posteo.de>
* Nathan <bonnemainsnathan@gmail.com>
* MyTimeKill <26295589+MyTimeKill@users.noreply.github.com>
* Molnár Barnabás <nsd4rkn3ss@gmail.com>
* Moarc <aldwulf@gmail.com>
* Michal Novotny <mignov@gmail.com>
* Mattias Münster <mattiasmun@gmail.com>
* Mattherix <mattherix@protonmail.com>
* Martin <ritualz@users.noreply.github.com>
* marco.altomonte <marco.altomonte@gmail.com>
* LizardWithHat <martin.betcher@gmail.com>
* Le Poisson Libre <services@spika.xyz>
* krzys_h <krzys_h@interia.pl>
* Konrad Iturbe <KonradIT@users.noreply.github.com>
* Jesús <zaagur@gmail.com>
* Jesús F <jfmiguel@wanadoo.es>
* Irul <wedesignthing@gmail.com>
* ifurther <i.further.5.4@gmail.com>
* homocomputeris <homocomputeris+git@gmail.com>
* Francesco Franchina <cescus92@gmail.com>
* fparri <fparri@noreply.codeberg.org>
* exit-failure <hakrala@web.de>
* Dreamwalker <aristojeff@gmail.com>
* Dario Lopez-Kästen <cl2dlope@gmail.com>
* Da Pa <da.pa@noreply.codeberg.org>
* DanialHanif <danialhanif@outlook.com>
* Cristian Alfano <c.alfano@outlook.it>
* criogenic <criogenic@gmail.com>
* chabotsi <chabotsi+github@chabotsi.fr>
* Avamander <Avamander@users.noreply.github.com>
* AnthonyDiGirolamo <anthony.digirolamo@gmail.com>
* Anonymous <noreply@weblate.org>
* Andreas Kromke <Andreas.Kromke@web.de>
* Ainārs <ainars71@inbox.lv>
* Ⲇⲁⲛⲓ Φi <daniphii@outlook.com>
* Your Name <you@example.com>
* Yar <yaroslav.isakov@gmail.com>
* xzovy <caleb@caleb-cooper.net>
* xphnx <xphnx@users.noreply.github.com>
* Xavier RENE-CORAIL <xavier.renecorail@gmail.com>
* xaos <xaos@noreply.codeberg.org>
* w2q <w2q@noreply.codeberg.org>
* Vitaliy Shuruta <vshuruta@gmail.com>
* veecue <veecue@ventos.tk>
* Unixware <csynt@hotmail.com>
* Triet Pham <triet.phm@gmail.com>
* Tomer Rosenfeld <tomerosenfeld007@gmail.com>
* Tomas Radej <tradej@redhat.com>
* Toby Murray <toby.murray+gitkraken@protonmail.com>
* t-m-w <t-m-w-codeberg@galac.tk>
* tiparega <11555126+tiparega@users.noreply.github.com>
* TinfoilSubmarine <tinfoilsubmarine@noreply.codeberg.org>
* Thiago Rodrigues <thiaguinho.the@gmail.com>
* Tarik Sekmen <tarik@ilixi.org>
* Szymon Tomasz Stefanek <s.stefanek@gmail.com>
* szilardx <15869670+szilardx@users.noreply.github.com>
* Swann Martinet <swann.ranskassa@laposte.net>
* Stan Gomin <stan@gomin.me>
* SinMan <emilio.galvan@gmail.com>
* Sergio Lopez <slp@sinrega.org>
* S Dantas <dantasosteney@gmail.com>
* Santiago Benalcázar <santiagosdbc@gmail.com>
* Samuel Carvalho de Araújo <samuelnegro12345@gmail.com>
* Sami Alaoui <4ndroidgeek@gmail.com>
* Roxystar <roxystar@arcor.de>
* Roman Plevka <rplevka@redhat.com>
* rober <rober@prtl.nodomain.net>
* Rivo Zängov <rivozangov@gmail.com>
* rimasx <riks_12@hot.ee>
* Richard Finegold <goldfndr+git@gmail.com>
* Retew <salau@tutanota.com>
* redking <redking974@gmail.com>
* Quallenauge <Hamsi2k@freenet.de>
* Q-er <9142398+Q-er@users.noreply.github.com>
* Perflyst <mail@perflyst.de>
* Pavel Motyrev <legioner.r@gmail.com>
* Pauli Salmenrinne <susundberg@gmail.com>
* pangwalla <pangwalla@noreply.codeberg.org>
* Pander <pander@users.sourceforge.net>
* Ondřej Sedláček <ond.sedlacek@gmail.com>
* Olivier Bloch <blochjunior@gmail.com>
* Nur Aiman Fadel <nuraiman@gmail.com>
* Nikolai Sinyov <nikolay.sinyov@yandex.ru>
* Nicolò Balzarotti <anothersms@gmail.com>
* Nephiel <Nephiel@users.noreply.github.com>
* Natanael Arndt <arndtn@gmail.com>
* Nabil BENDAFI <nabil@bendafi.fr>
* Mirko Covizzi <mrkcvzz@gmail.com>
* Milo Ivir <mail@milotype.de>
* Mike van Rossum <mike@vanrossum.net>
* Michal Novak <michal.novak@post.cz>
* michaelneu <git@michaeln.eu>
* Lesur Frederic <contact@memiks.fr>
* McSym28 <McSym28@users.noreply.github.com>
* MaxL <z60loa8qw3umzu3@my10minutemail.com>
* maxirnilian <maxirnilian@users.noreply.github.com>
* Maxim Baz <git@maximbaz.com>
* Matej Drobnič <matejdro@gmail.com>
* Marvin D <mave95@posteo.de>
* Martin Piatka <chachacha2323@gmail.com>
* Margreet <margreetkeelan@gmail.com>
* Marc Schlaich <marc.schlaich@googlemail.com>
* Marco Alberto Diosdado Nava <betoxxdiosnava@gmail.com>
* Marco A <35718078+TomasCartman@users.noreply.github.com>
* Marc Nause <marc.nause@audioattack.de>
* Marc Laporte <marc@laporte.name>
* Marcin <ml.cichy@gmail.com>
* Marcel pl (m4rcel) <marcel.garbarczyk@gmail.com>
* Manuel Soler <vg8020@gmail.com>
* Manuel Ruß <manuel_russ@dismail.de>
* mangel <mangelcursos@gmail.com>
* magimel.francois <magimel.francois@gmail.com>
* Maciej Kuśnierz <>
* m4sk1n <me@m4sk.in>
* Luiz Felipe das Neves Lopes <androidfelipe23@gmail.com>
* Luis zas <dalues@gmail.com>
* luca sain <luca.sain@outlook.com>
* lucanomax <lucano.valo@gmail.com>
* Leonardo Amaral <contato@leonardoamaral.com.br>
* Leo bonilla <leo_lf9@hotmail.com>
* Lejun <adrienzhang@hotmail.com>
* lazarosfs <lazarosfs@csd.auth.gr>
* ladbsoft <30509719+ladbsoft@users.noreply.github.com>
* Kristjan Räts <kristjanrats@gmail.com>
* kevlarcade <kevlarcade@gmail.com>
* Kevin Richter <me@kevinrichter.nl>
* keeshii <keeshii@ptcg.eu>
* Kaz Wolfe <root@kazwolfe.io>
* Kasha <kasha_malaga@hotmail.com>
* kalaee <alex.kalaee@gmail.com>
* Julian Lam <julian@nodebb.org>
* jugendhacker <jugendhacker@gmail.com>
* Joseph Kim <official.jkim@gmail.com>
* jonnsoft <>
* Johannes Tysiak <vinyl@users.sf.net>
* Joan Perals <jperals@protonmail.com>
* JF <jf@codingfield.com>
* jcrode <46062294+jcrode@users.noreply.github.com>
* Jan Lolek <janlolek@seznam.cz>
* Jakub Jelínek <jakub.jelinek@gmail.com>
* Izzy <izzy@qumran.org>
* iwonder <hosted.weblate.org@heychris.eu>
* Ivan <ivan_tizhanin@mail.ru>
* Igor Polyakov <igorpolyakov@protonmail.com>
* Hüseyin Aslan <ha098784@gmail.com>
* hr-sales <hericsonregis@hotmail.com>
* Hirnchirurg <anonymous11@posteo.net>
* Hasan Ammar <ammarh@gmail.com>
* HardLight <hardlightxda@gmail.com>
* Hanhan Husna <matcherapy@gmail.com>
* hackoder <hackoder@noreply.codeberg.org>
* Grzegorz Dznsk <grantmlody96@gmail.com>
* Gleb Chekushin <mail@glebchek.com>
* Giuseppe <giuseppe.parasilitipalumbo@studium.unict.it>
* Gideão Gomes Ferreira <trjctr@gmail.com>
* GabO <gabosuelto@gmail.com>
* Gabe Schrecker <gabe@pbrb.co.uk>
* freezed-or-frozen <freezed.or.frozen@gmail.com>
* Frank Slezak <KazWolfe@users.noreply.github.com>
* Florian Beuscher <florianbeuscher@gmail.com>
* Étienne Deparis <etienne@depar.is>
* Estébastien Robespi <estebastien@mailbox.org>
* Edoardo Tronconi <edoardo.tronconi@gmail.com>
* Dougal19 <4662351+Dougal19@users.noreply.github.com>
* Dmytro Bielik <mitrandir.hex@gmail.com>
* DerFetzer <kontakt@der-fetzer.de>
* Deactivated Account <diastasis@gmail.com>
* Davis Mosenkovs <davikovs@gmail.com>
* Daniel Hauck <maill@dhauck.eu>
* cokecodecock <lights1140977891@163.com>
* C O <cosmin.oprisan@gmail.com>
* clach04 <Chris.Clark@actian.com>
* Chris Perelstein <chris.perelstein@gmail.com>
* Carlos Ferreira <calbertoferreira@gmail.com>
* C0rn3j <spleefer90@gmail.com>
* ButterflyOfFire <ButterflyOfFire@protonmail.com>
* bucala <marcel.bucala@gmail.com>
* boun <boun@gmx.de>
* Benjamin Kahlau <nyhkkbjyek@roanapur.de>
* batataspt@gmail.com <batataspt@gmail.com>
* atkyritsis <at.kyritsis@gmail.com>
* Artem <KovalevArtem.ru@gmail.com>
* apre <adrienpre+github@gmail.com>
* Aniruddha Adhikary <aniruddha@adhikary.net>
* angelpup <angelpup@noreply.codeberg.org>
* Andrzej Surowiec <emeryth@gmail.com>
* andrewlytvyn <indusfreelancer@gmail.com>
* AndrewH <36428679+andrewheadricke@users.noreply.github.com>
* andre <andre.buesgen@yahoo.de>
* Allen B <28495335+Allen-B1@users.noreply.github.com>
* Alfeu Lucas Guedes dos Santos <alfeugds@gmail.com>
* Alex <leha-bot@yandex.ru>
* Alexey Afanasev <avafanasiev@gmail.com>
* Alexandra Sevostyanova <asevostyanova@gmail.com>
* aerowolf <aerowolf@tom.com>

And all the former Transifex translators, who cannot be listed automatically.

*To update the contributors list just run this file with bash. Prefix a name with % in .mailmap to set a contact as preferred*
