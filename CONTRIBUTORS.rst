.. 2>/dev/null
 names () 
 { 
 echo -e "\n exit;\n**Contributors (sorted by number of commits):**\n";
 git log --format='%aN:%ae' origin/master | grep -Ev "(anonymous:|FYG_.*_bot_ignore_me)" | sed 's/@users.github.com/@users.noreply.github.com/g' | awk 'BEGIN{FS=":"}{match ($1, /^(%)?(.*)/, n) ; ct[n[2]]+=1; if (n[1] ~ /%/ || e[n[2]] == "" ) { e[n[2]]=$2}}END{for (i in e) { n[i]=e[i];c[i]+=ct[i] }; for (a in e) print c[a]"\t* "a" <"n[a]">";}' | sort -n -r | cut -f 2-
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
* Jonas <jonasdcdm@posteo.net>
* Yaron Shahrabani <sh.yaron@gmail.com>
* protomors <protomors@gmail.com>
* postsorino <postsorino@krutt.org>
* ivanovlev <lion.ivanov@gmal.com>
* mueller-ma <matthias_mueller_lgz@t-online.de>
* Allan Nordhøy <epost@anotheragency.no>
* Julien Pivotto <roidelapluie@inuits.eu>
* youzhiran <2668760098@qq.com>
* Steffen Liebergeld <perl@gmx.org>
* Lem Dulfo <lemuel.dulfo@gmail.com>
* naofum <naofum@gmail.com>
* Felix Konstantin Maurer <maufl@maufl.de>
* Sergey Trofimov <sarg@sarg.org.ru>
* José Rebelo <joserebelo@outlook.com>
* JohnnySun <bmy001@gmail.com>
* Hadrián Candela <hadrian.candela@gmail.com>
* Uwe Hermann <uwe@hermann-uwe.de>
* TaaviE <taavi.eomae+weblate@gmail.com>
* Alberto <albertsal83@gmail.com>
* Vladislav Serkov <vladserkoff@protonmail.com>
* Vebryn <vebryn@gmail.com>
* Gergely Peidl <gergely@peidl.net>
* Bożydar <trening302@o2.pl>
* 0nse <0nse@users.noreply.github.com>
* Tijl Schepens <tijl.schepens@hotmail.com>
* masakoodaa <masakoodaa@protonmail.com>
* Lukas Veneziano <fs@venezilu.de>
* Kompact <joaorafael123@hotmail.com>
* Jasper <jespiex456@hotmail.com>
* Christian Fischer <sw-dev@computerlyrik.de>
* c4ndel4 <hadrian.candela@gmail.com>
* 6arms1leg <m.brnsfld@googlemail.com>
* Zhong Jianxin <azuwis@gmail.com>
* walkjivefly <mark@walkjivefly.com>
* Normano64 <per.bergqwist@gmail.com>
* nautilusx <mail.ka@mailbox.org>
* Minori Hiraoka (미노리) <minori@mnetwork.co.kr>
* Michal Novotny <mignov@gmail.com>
* mesnevi <shams@airpost.net>
* LL <lu.lecocq@free.fr>
* Jesús <zaagur@gmail.com>
* Gilles Émilien MOREL <contact@gilles-morel.fr>
* Avamander <Avamander@users.noreply.github.com>
* AnthonyDiGirolamo <anthony.digirolamo@gmail.com>
* Andreas Kromke <Andreas.Kromke@web.de>
* Ⲇⲁⲛⲓ Φi <daniphii@outlook.com>
* Yar <yaroslav.isakov@gmail.com>
* xzovy <caleb@caleb-cooper.net>
* xphnx <xphnx@users.noreply.github.com>
* Vitaliy Shuruta <vshuruta@gmail.com>
* Tomer Rosenfeld <tomerosenfeld007@gmail.com>
* Tomas Radej <tradej@redhat.com>
* Ted Stein <me@tedstein.net>
* Tarik Sekmen <tarik@ilixi.org>
* Szymon Tomasz Stefanek <s.stefanek@gmail.com>
* Sami Alaoui <4ndroidgeek@gmail.com>
* Roman Plevka <rplevka@redhat.com>
* rober <rober@prtl.nodomain.net>
* redking <redking974@gmail.com>
* Quallenauge <Hamsi2k@freenet.de>
* Pavel Motyrev <legioner.r@gmail.com>
* Pascal <pascal.tannich@gmail.com>
* Olexandr Nesterenko <olexn@ukr.net>
* NotAFIle <nota@notafile.com>
* NotAFile <notafile@gmail.com>
* Nicolò Balzarotti <anothersms@gmail.com>
* Natanael Arndt <arndtn@gmail.com>
* Michal Novak <michal.novak@post.cz>
* michaelneu <git@michaeln.eu>
* McSym28 <McSym28@users.noreply.github.com>
* MaxL <z60loa8qw3umzu3@my10minutemail.com>
* Marc Schlaich <marc.schlaich@googlemail.com>
* Leonardo Amaral <contato@leonardoamaral.com.br>
* lazarosfs <lazarosfs@csd.auth.gr>
* ladbsoft <30509719+ladbsoft@users.noreply.github.com>
* kevlarcade <kevlarcade@gmail.com>
* Kevin Richter <me@kevinrichter.nl>
* Kaz Wolfe <root@kazwolfe.io>
* Kasha <kasha_malaga@hotmail.com>
* Joseph Kim <official.jkim@gmail.com>
* Jan Lolek <janlolek@seznam.cz>
* Ivan <ivan_tizhanin@mail.ru>
* Hasan Ammar <ammarh@gmail.com>
* Gideão Gomes Ferreira <trjctr@gmail.com>
* Gabe Schrecker <gabe@pbrb.co.uk>
* freezed-or-frozen <freezed.or.frozen@gmail.com>
* Frank Slezak <KazWolfe@users.noreply.github.com>
* Davis Mosenkovs <davikovs@gmail.com>
* Daniel Hauck <maill@dhauck.eu>
* criogenic <criogenic@gmail.com>
* Chris Perelstein <chris.perelstein@gmail.com>
* Carlos Ferreira <calbertoferreira@gmail.com>
* bucala <marcel.bucala@gmail.com>
* batataspt@gmail.com <batataspt@gmail.com>
* atkyritsis <at.kyritsis@gmail.com>
* andre <andre.buesgen@yahoo.de>
* Alexey Afanasev <avafanasiev@gmail.com>

And all the Transifex translators, which I cannot automatically list, at the moment.

*To update the contributors list just run this file with bash. Prefix a name with % in .mailmap to set a contact as preferred*
