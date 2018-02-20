.. 2>/dev/null
 names () 
 { 
 echo -e "\n exit;\n**Contributors (sorted by number of commits):**\n";
 git log --format='%aN:%ae' origin/master | grep -Ev "FYG_.*_bot_ignore_me" | sed 's/@users.github.com/@users.noreply.github.com/g' | awk 'BEGIN{FS=":"}{ct[$1]+=1;if (length($2) > length(e[$1])) {e[$1]=$2}}END{for (i in e)  { n[i]=e[i];c[i]+=ct[i] }; for (a in e) print c[a]"\t* "a" <"n[a]">";}' | sort -n -r | cut -f 2-
 }
 quine () 
 { 
 { 
 echo ".. 2>/dev/null";
 declare -f names | sed -e 's/^[[:space:]]*/ /';
 declare -f quine | sed -e 's/^[[:space:]]*/ /';
 echo -e " quine\n";
 names;
 echo -e "\nAnd all the Transifex translators, which I cannot automatically list, at the moment.\n\n*To update the contributors list just run this file with bash*"
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
* ivanovlev <lion.ivanov@gmal.com>
* Julien Pivotto <roidelapluie@inuits.eu>
* Steffen Liebergeld <perl@gmx.org>
* Lem Dulfo <lemuel.dulfo@gmail.com>
* Sergey Trofimov <sarg@sarg.org.ru>
* JohnnySun <bmy001@gmail.com>
* Uwe Hermann <uwe@hermann-uwe.de>
* Alberto <albertsal83@gmail.com>
* 0nse <0nse@users.noreply.github.com>
* Gergely Peidl <gergely@peidl.net>
* Christian Fischer <sw-dev@computerlyrik.de>
* 6arms1leg <m.brnsfld@googlemail.com>
* walkjivefly <mark@walkjivefly.com>
* Normano64 <per.bergqwist@gmail.com>
* Avamander <Avamander@users.noreply.github.com>
* Ⲇⲁⲛⲓ Φi <daniphii@outlook.com>
* Yar <yaroslav.isakov@gmail.com>
* Yaron Shahrabani <sh.yaron@gmail.com>
* xzovy <caleb@caleb-cooper.net>
* xphnx <xphnx@users.noreply.github.com>
* Tarik Sekmen <tarik@ilixi.org>
* Szymon Tomasz Stefanek <s.stefanek@gmail.com>
* Roman Plevka <rplevka@redhat.com>
* rober <rober@prtl.nodomain.net>
* Nicolò Balzarotti <anothersms@gmail.com>
* Natanael Arndt <arndtn@gmail.com>
* Marc Schlaich <marc.schlaich@googlemail.com>
* kevlarcade <kevlarcade@gmail.com>
* Kevin Richter <me@kevinrichter.nl>
* Kasha <kasha_malaga@hotmail.com>
* Ivan <ivan_tizhanin@mail.ru>
* Hasan Ammar <ammarh@gmail.com>
* Gilles Émilien MOREL <contact@gilles-morel.fr>
* Daniel Hauck <maill@dhauck.eu>
* Chris Perelstein <chris.perelstein@gmail.com>
* Carlos Ferreira <calbertoferreira@gmail.com>
* atkyritsis <at.kyritsis@gmail.com>
* andre <andre.buesgen@yahoo.de>
* Alexey Afanasev <avafanasiev@gmail.com>

And all the Transifex translators, which I cannot automatically list, at the moment.

*To update the contributors list just run this file with bash*
