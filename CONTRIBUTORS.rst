.. 2>/dev/null
 names () 
 { 
 echo -e "\n exit;\n**Contributors (sorted by number of commits):**\n";
 git log --all --format='%aN:%aE' | sed 's/@users.github.com/@users.noreply.github.com/g' | awk 'BEGIN{FS=":"}{ct[$2]+=1;if (length($1) > length(e[$2])) {e[$2]=$1}}END{for (i in e)  { n[e[i]]=i;c[e[i]]+=ct[i] }; for (a in n) print c[a]"\t* "a" <"n[a]">";}' | sort -n -r | cut -f 2-
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
* Julien Pivotto <roidelapluie@inuits.eu>
* Steffen Liebergeld <perl@gmx.org>
* Lem Dulfo <lemuel.dulfo@gmail.com>
* Sergey Trofimov <sarg@sarg.org.ru>
* JohnnySun <bmy001@gmail.com>
* Uwe Hermann <uwe@hermann-uwe.de>
* Gergely Peidl <gergely@peidl.net>
* 0nse <0nse@users.noreply.github.com>
* Christian Fischer <sw-dev@computerlyrik.de>
* Normano64 <per.bergqwist@gmail.com>
* Ⲇⲁⲛⲓ Φi <daniphii@outlook.com>
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
* Gilles MOREL <contact@gilles-morel.fr>
* Gilles Émilien MOREL <Almtesh@users.noreply.github.com>
* Chris Perelstein <chris.perelstein@gmail.com>
* Carlos Ferreira <calbertoferreira@gmail.com>
* atkyritsis <at.kyritsis@gmail.com>
* andre <andre.buesgen@yahoo.de>
* Alexey Afanasev <avafanasiev@gmail.com>
* 6arms1leg <m.brnsfld@googlemail.com>

And all the Transifex translators, which I cannot automatically list, at the moment.

*To update the contributors list just run this file with bash*
