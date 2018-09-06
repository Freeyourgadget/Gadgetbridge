.. 2>/dev/null
 names () 
 { 
 echo -e "\n exit;\n**Contributors (sorted by number of commits):**\n";
 git log --format='%aN:%aE' origin/master | grep -Ev "(anonymous:|FYG_.*_bot_ignore_me)" | sed 's/@users.github.com/@users.noreply.github.com/g' | awk 'BEGIN{FS=":"}{ct[$1]+=1;e[$1]=$2}END{for (i in e)  { n[i]=e[i];c[i]+=ct[i] }; for (a in e) print c[a]"\t* "a" <"n[a]">";}' | sort -n -r | cut -f 2-
 }
 quine () 
 { 
 { 
 echo ".. 2>/dev/null";
 declare -f names | sed -e 's/^[[:space:]]*/ /';
 declare -f quine | sed -e 's/^[[:space:]]*/ /';
 echo -e " quine\n";
 names;
 echo -e "\nAnd all the Transifex translators, which I cannot automatically list, at the moment.\n\n*To update the contributors list just run this file with bash. Prefix a name with % in .mailmap to set a contact as preferred*"
 } > CONTRIBUTORS.rst;
 exit
 }
 quine


 exit;
**Contributors (sorted by number of commits):**

* Andreas Shimokawa <shimokawa@fsfe.org>
* Carsten Pfeiffer <cpfeiffer@users.noreply.github.com>
* Daniele Gobbetti <daniele+github@gobbetti.name>
* João Paulo Barraca <jpbarraca@gmail.com>
* Yaron Shahrabani <sh.yaron@gmail.com>
* Jonas <jonasdcdm@posteo.net>
* postsorino <postsorino@krutt.org>
* Sebastian Kranz <tklightforce@googlemail.com>
* Vadim Kaushan <admin@disasm.info>
* Allan Nordhøy <epost@anotheragency.no>
* protomors <protomors@gmail.com>
* José Rebelo <joserebelo@outlook.com>
* TaaviE <taavi.eomae+weblate@gmail.com>
* mueller-ma <mueller-ma@users.noreply.github.com>
* ivanovlev <ivanovlev@mail.ru>
* naofum <naofum@gmail.com>
* youzhiran <2668760098@qq.com>
* Tijl Schepens <tijl.schepens@hotmail.com>
* mesnevi <shams@airpost.net>
* Julien Pivotto <roidelapluie@inuits.eu>
* Taavi Eomäe <taavi.eomae+github@gmail.com>
* Steffen Liebergeld <perl@gmx.org>
* Lem Dulfo <lemuel.dulfo@gmail.com>
* Hadrián Candela <hadrian.candela@gmail.com>
* Felix Konstantin Maurer <maufl@maufl.de>
* Sergey Trofimov <sarg@sarg.org.ru>
* Robert Barat <rbarat07@gmail.com>
* Pavel Elagin <pelagin@techcd.ru>
* JohnnySun <bmy001@gmail.com>
* Uwe Hermann <uwe@hermann-uwe.de>
* Kranz <Kranz>
* Edoardo Rosa <edoardo.rosa90@gmail.com>
* Alberto <albertsal83@gmail.com>
* Vladislav Serkov <vladserkoff@protonmail.com>
* Vebryn <vebryn@gmail.com>
* Gilles Émilien MOREL <contact@gilles-morel.fr>
* Gergely Peidl <gergely@peidl.net>
* Emre <wenigerpluesch@mailbox.org>
* Bożydar <trening302@o2.pl>
* 0nse <0nse@users.noreply.github.com>
* Максим Якимчук <xpinovo@gmail.com>
* Rimas Raguliūnas <rarimas@gmail.com>
* nautilusx <mail.ka@mailbox.org>
* masakoodaa <masakoodaa@protonmail.com>
* Marius Cornescu <marius_cornescu@yahoo.com>
* Lukas Veneziano <fs@venezilu.de>
* Kompact <joaorafael123@hotmail.com>
* K0L0B0G <github@gorobav.ru>
* Jasper <jespiex456@hotmail.com>
* Christian Fischer <sw-dev@computerlyrik.de>
* c4ndel4 <hadrian.candela@gmail.com>
* 6arms1leg <m.brnsfld@googlemail.com>
* Zhong Jianxin <azuwis@gmail.com>
* walkjivefly <mark@walkjivefly.com>
* Thomas <tutonis@gmail.com>
* Ted Stein <me@tedstein.net>
* petronovak <petro.novak@gmail.com>
* Pascal <pascal.tannich@gmail.com>
* NotAFIle <nota@notafile.com>
* Normano64 <per.bergqwist@gmail.com>
* NicoBuntu <nicolas__du95@hotmail.fr>
* Minori Hiraoka (미노리) <minori@mnetwork.co.kr>
* Michal Novotny <mignov@gmail.com>
* Martin <ritualz@users.noreply.github.com>
* LL <lu.lecocq@free.fr>
* Jesús <zaagur@gmail.com>
* exit-failure <hakrala@web.de>
* Denis <korden@sky-play.ru>
* Avamander <Avamander@users.noreply.github.com>
* AnthonyDiGirolamo <anthony.digirolamo@gmail.com>
* Andreas Kromke <Andreas.Kromke@web.de>
* Ⲇⲁⲛⲓ Φi <daniphii@outlook.com>
* Your Name <you@example.com>
* Yar <yaroslav.isakov@gmail.com>
* xzovy <caleb@caleb-cooper.net>
* xphnx <xphnx@users.noreply.github.com>
* Vitaliy Shuruta <vshuruta@gmail.com>
* Vincèn PUJOL <vincen@vincen.org>
* Tomer Rosenfeld <tomerosenfeld007@gmail.com>
* Tomas Radej <tradej@redhat.com>
* tiparega <11555126+tiparega@users.noreply.github.com>
* Tarik Sekmen <tarik@ilixi.org>
* Szymon Tomasz Stefanek <s.stefanek@gmail.com>
* szilardx <15869670+szilardx@users.noreply.github.com>
* Sergio Lopez <slp@sinrega.org>
* Sami Alaoui <4ndroidgeek@gmail.com>
* Roman Plevka <rplevka@redhat.com>
* rober <rober@prtl.nodomain.net>
* redking <redking974@gmail.com>
* Quallenauge <Hamsi2k@freenet.de>
* Pavel Motyrev <legioner.r@gmail.com>
* Olexandr Nesterenko <olexn@ukr.net>
* Nicolò Balzarotti <anothersms@gmail.com>
* Natanael Arndt <arndtn@gmail.com>
* Molnár Barnabás <nsd4rkn3ss@gmail.com>
* Moarc <aldwulf@gmail.com>
* Mike van Rossum <mike@vanrossum.net>
* Michal Novak <michal.novak@post.cz>
* michaelneu <git@michaeln.eu>
* McSym28 <McSym28@users.noreply.github.com>
* MaxL <z60loa8qw3umzu3@my10minutemail.com>
* maxirnilian <maxirnilian@users.noreply.github.com>
* Martin Piatka <chachacha2323@gmail.com>
* Margreet <margreetkeelan@gmail.com>
* Marc Schlaich <marc.schlaich@googlemail.com>
* Marcel pl (m4rcel) <marcel.garbarczyk@gmail.com>
* Manuel Soler <vg8020@gmail.com>
* Luiz Felipe das Neves Lopes <androidfelipe23@gmail.com>
* Leonardo Amaral <contato@leonardoamaral.com.br>
* lazarosfs <lazarosfs@csd.auth.gr>
* ladbsoft <30509719+ladbsoft@users.noreply.github.com>
* Kristjan Räts <kristjanrats@gmail.com>
* Konrad Iturbe <KonradIT@users.noreply.github.com>
* kevlarcade <kevlarcade@gmail.com>
* Kevin Richter <me@kevinrichter.nl>
* Kaz Wolfe <root@kazwolfe.io>
* Kasha <kasha_malaga@hotmail.com>
* kalaee <alex.kalaee@gmail.com>
* Joseph Kim <official.jkim@gmail.com>
* jonnsoft <>
* Jan Lolek <janlolek@seznam.cz>
* Jakub Jelínek <jakub.jelinek@gmail.com>
* Ivan <ivan_tizhanin@mail.ru>
* Hasan Ammar <ammarh@gmail.com>
* Grzegorz Dznsk <grantmlody96@gmail.com>
* Gilles MOREL <contact@gilles-morel.fr>
* Gideão Gomes Ferreira <trjctr@gmail.com>
* Gabe Schrecker <gabe@pbrb.co.uk>
* freezed-or-frozen <freezed.or.frozen@gmail.com>
* Frank Slezak <KazWolfe@users.noreply.github.com>
* Dreamwalker <aristojeff@gmail.com>
* Dougal19 <4662351+Dougal19@users.noreply.github.com>
* Davis Mosenkovs <davikovs@gmail.com>
* Daniel Hauck <maill@dhauck.eu>
* dakhnod <dakhnod@gmail.com>
* criogenic <criogenic@gmail.com>
* clach04 <Chris.Clark@actian.com>
* Chris Perelstein <chris.perelstein@gmail.com>
* chabotsi <chabotsi+github@chabotsi.fr>
* Carlos Ferreira <calbertoferreira@gmail.com>
* bucala <marcel.bucala@gmail.com>
* boun <boun@gmx.de>
* batataspt@gmail.com <batataspt@gmail.com>
* atkyritsis <at.kyritsis@gmail.com>
* Aniruddha Adhikary <aniruddha@adhikary.net>
* andrewlytvyn <indusfreelancer@gmail.com>
* AndrewH <36428679+andrewheadricke@users.noreply.github.com>
* andre <andre.buesgen@yahoo.de>
* Allen B <28495335+Allen-B1@users.noreply.github.com>
* Alexey Afanasev <avafanasiev@gmail.com>

And all the Transifex translators, which I cannot automatically list, at the moment.

*To update the contributors list just run this file with bash. Prefix a name with % in .mailmap to set a contact as preferred*
