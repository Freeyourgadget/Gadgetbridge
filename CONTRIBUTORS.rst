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
* Allan Nordhøy <epost@anotheragency.no>
* postsorino <postsorino@krutt.org>
* Jonas <jonasdcdm@posteo.net>
* Roi Greenberg <roigreenberg@gmail.com>
* Sebastian Kranz <tklightforce@googlemail.com>
* Vadim Kaushan <admin@disasm.info>
* protomors <protomors@gmail.com>
* José Rebelo <joserebelo@outlook.com>
* mesnevi <shams@airpost.net>
* naofum <naofum@gmail.com>
* youzhiran <2668760098@qq.com>
* TaaviE <taavi.eomae+weblate@gmail.com>
* mueller-ma <mueller-ma@users.noreply.github.com>
* ivanovlev <ivanovlev@mail.ru>
* Tijl Schepens <tijl.schepens@hotmail.com>
* Hadrián Candela <hadrian.candela@gmail.com>
* Julien Pivotto <roidelapluie@inuits.eu>
* Andreas Böhler <dev@aboehler.at>
* 陈少举 <oshirisu.red@gmail.com>
* Taavi Eomäe <taavi.eomae+github@gmail.com>
* Steffen Liebergeld <perl@gmx.org>
* Pavel Elagin <pelagin@techcd.ru>
* Lem Dulfo <lemuel.dulfo@gmail.com>
* Matthieu Baerts <matttbe@gmail.com>
* Felix Konstantin Maurer <maufl@maufl.de>
* Utsob Roy <uroybd@gmail.com>
* Sergey Trofimov <sarg@sarg.org.ru>
* Full Name <petr+weblate@linuks.cz>
* Robert Barat <rbarat07@gmail.com>
* JohnnySun <bmy001@gmail.com>
* Uwe Hermann <uwe@hermann-uwe.de>
* Kranz <Kranz>
* Gilles Émilien MOREL <contact@gilles-morel.fr>
* Edoardo Rosa <edoardo.rosa90@gmail.com>
* Bożydar <trening302@o2.pl>
* Alberto <albertsal83@gmail.com>
* Vladislav Serkov <vladserkoff@protonmail.com>
* Vebryn <vebryn@gmail.com>
* M. Hadi <hhhadddi@yahoo.com>
* Giuseppe Caliendo <giuseppe.caliendo@gmail.com>
* Gergely Peidl <gergely@peidl.net>
* Emre <wenigerpluesch@mailbox.org>
* Elwood <elwood21@gmail.com>
* AndrewBedscastle <1462953+AndrewBedscastle@users.noreply.github.com>
* abettenburg <a.bettenburg@gmail.com>
* 0nse <0nse@users.noreply.github.com>
* Максим Якимчук <xpinovo@gmail.com>
* Rimas Raguliūnas <rarimas@gmail.com>
* nautilusx <mail.ka@mailbox.org>
* Minori Hiraoka (미노리) <minori@mnetwork.co.kr>
* masakoodaa <masakoodaa@protonmail.com>
* Marius Cornescu <marius_cornescu@yahoo.com>
* Lukas Veneziano <fs@venezilu.de>
* LL <lu.lecocq@free.fr>
* Kompact <joaorafael123@hotmail.com>
* K0L0B0G <github@gorobav.ru>
* Johann C. Rode <jcrode@ece.ucsb.edu>
* Jasper <jespiex456@hotmail.com>
* Dikay900 <dark900@xyz.de>
* Christian Fischer <sw-dev@computerlyrik.de>
* 6arms1leg <m.brnsfld@googlemail.com>
* Zhong Jianxin <azuwis@gmail.com>
* walkjivefly <mark@walkjivefly.com>
* WaldiS <admin@sto.ugu.pl>
* Thomas <tutonis@gmail.com>
* Ted Stein <me@tedstein.net>
* ssantos <ssantos@web.de>
* Sebastian Obrusiewicz <sobrus2@o2.pl>
* Ranved Sticon <the7bulk@gmail.com>
* petronovak <petro.novak@gmail.com>
* Petr Kadlec <mormegil@centrum.cz>
* Pascal <pascal.tannich@gmail.com>
* NotAFIle <nota@notafile.com>
* Normano64 <per.bergqwist@gmail.com>
* NicoBuntu <nicolas__du95@hotmail.fr>
* Moarc <aldwulf@gmail.com>
* Michal Novotny <mignov@gmail.com>
* Martin <ritualz@users.noreply.github.com>
* Louis-Marie Croisez <louis.croisez@gmail.com>
* Jesús <zaagur@gmail.com>
* Irul <wedesignthing@gmail.com>
* HenRy <helge1o1o1@gmail.com>
* exit-failure <hakrala@web.de>
* Dreamwalker <aristojeff@gmail.com>
* Denis <korden@sky-play.ru>
* Avamander <Avamander@users.noreply.github.com>
* AnthonyDiGirolamo <anthony.digirolamo@gmail.com>
* Andreas Kromke <Andreas.Kromke@web.de>
* Ⲇⲁⲛⲓ Φi <daniphii@outlook.com>
* Your Name <you@example.com>
* Yar <yaroslav.isakov@gmail.com>
* xzovy <caleb@caleb-cooper.net>
* xphnx <xphnx@users.noreply.github.com>
* Xavier RENE-CORAIL <xavier.renecorail@gmail.com>
* Vitaliy Shuruta <vshuruta@gmail.com>
* Vincèn PUJOL <vincen@vincen.org>
* veecue <veecue@ventos.tk>
* Tomer Rosenfeld <tomerosenfeld007@gmail.com>
* Tomas Radej <tradej@redhat.com>
* tiparega <11555126+tiparega@users.noreply.github.com>
* Tarik Sekmen <tarik@ilixi.org>
* Szymon Tomasz Stefanek <s.stefanek@gmail.com>
* szilardx <15869670+szilardx@users.noreply.github.com>
* Stan Gomin <stan@gomin.me>
* SinMan <emilio.galvan@gmail.com>
* Sergio Lopez <slp@sinrega.org>
* S Dantas <dantasosteney@gmail.com>
* Sami Alaoui <4ndroidgeek@gmail.com>
* Roman Plevka <rplevka@redhat.com>
* rober <rober@prtl.nodomain.net>
* redking <redking974@gmail.com>
* Quallenauge <Hamsi2k@freenet.de>
* Pavel Motyrev <legioner.r@gmail.com>
* Pavel <elagin.pasha@gmail.com>
* Olexandr Nesterenko <olexn@ukr.net>
* Nicolò Balzarotti <anothersms@gmail.com>
* Natanael Arndt <arndtn@gmail.com>
* Nabil BENDAFI <nabil@bendafi.fr>
* Molnár Barnabás <nsd4rkn3ss@gmail.com>
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
* Johannes Tysiak <vinyl@users.sf.net>
* jcrode <46062294+jcrode@users.noreply.github.com>
* Jan Lolek <janlolek@seznam.cz>
* Jakub Jelínek <jakub.jelinek@gmail.com>
* Ivan <ivan_tizhanin@mail.ru>
* Hüseyin Aslan <ha098784@gmail.com>
* hr-sales <hericsonregis@hotmail.com>
* Hirnchirurg <anonymous11@posteo.net>
* Hasan Ammar <ammarh@gmail.com>
* Grzegorz Dznsk <grantmlody96@gmail.com>
* Gilles MOREL <contact@gilles-morel.fr>
* Gideão Gomes Ferreira <trjctr@gmail.com>
* Gabe Schrecker <gabe@pbrb.co.uk>
* freezed-or-frozen <freezed.or.frozen@gmail.com>
* Frank Slezak <KazWolfe@users.noreply.github.com>
* Francesco Franchina <cescus92@gmail.com>
* Edoardo Tronconi <edoardo.tronconi@gmail.com>
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
* Benjamin Kahlau <nyhkkbjyek@roanapur.de>
* batataspt@gmail.com <batataspt@gmail.com>
* atkyritsis <at.kyritsis@gmail.com>
* apre <adrienpre+github@gmail.com>
* Aniruddha Adhikary <aniruddha@adhikary.net>
* andrewlytvyn <indusfreelancer@gmail.com>
* AndrewH <36428679+andrewheadricke@users.noreply.github.com>
* andre <andre.buesgen@yahoo.de>
* Allen B <28495335+Allen-B1@users.noreply.github.com>
* Alfeu Lucas Guedes dos Santos <alfeugds@gmail.com>
* Alexey Afanasev <avafanasiev@gmail.com>
* Alexandra Sevostyanova <asevostyanova@gmail.com>

And all the Transifex translators, which I cannot automatically list, at the moment.

*To update the contributors list just run this file with bash. Prefix a name with % in .mailmap to set a contact as preferred*
