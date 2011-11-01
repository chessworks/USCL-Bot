#!/bin/sh
rm Players.html
wget --output-document=Players.html --output-file=/dev/null 'http://www.uschessleague.com/Players.html'
cat Players.html | sed --regexp-extended '
	/\/index.html/,/\/rules\.html/ d
	/\.html/ ! d
	s/^.*http/http/
	s/".*$//
' | sed '
	1,16 p
	18~2 p
	d
' > Links-All.txt
head --lines=+16 Links-All.txt > Links-Teams.txt
tail --lines=+17 Links-All.txt > Links-Players.txt
cat Players.html | sed '
	/^$/ d
	s/\t/        /g
' | sed --regexp-extended '
	/^$/ d
	0,/http:\/\/www\.cafepress\.com\/uschessleague/ d
	/\/index\.html/,$ d
	/\.html/ ! d
	N
	N
	N
	s/[\r\n]+/ /g
	s/^.*\/([A-Za-z]+)\.html.*$/\1	/
	:loop
	N
	/uschessleague/ ! b loop
	N
	N
	N
	s/[\r\n]+/ /g
	s/ *</</
	s/> */>/
	/>([0-9]+)</ ! d
	s/	[^	]*http/	http/
	s/<\/a>.*>([0-9]+)<[^	]*/	\1/
	s/">/	/
	s/  */ /g
	s/(http:[^ ]*)	(W?[A-Z]M)? ?(.*)$/\1	\2	\3/
	s/\((W?[A-Z]M) \)/(\1)/
	s/^(.*http:.*com\/)([^/	.]*)(\.html)/\2	\1\2\3/
	s/ *<[^>]*> *//g
	s/ +$//
	s/^ +//
	s/^([^\t]*) *\t *([^\t]*) *\t *([^\t]*) *\t *([^\t]*) *\t *([^\t]*) *\t *([^\t]*)$/player.\1.name=\5\
player.\1.team=\2\
player.\1.rating=\6\
player.\1.titles=(\4)\
player.\1.website=\3\
/
	s/titles=\(\)/titles=/
' > New-Players.txt
cat New-Players.txt | sed --regexp-extended '
	1 i\
	\/\\\.name=\/ ! d
	$ a\
	d
	/\.name=/ ! d
	s/^player\.([^.]*)\.name=(.*)$/	\/name=\2\$\/ {\
		s\/\^player\\\.\(\[^\.\]\*\)\\\.name=\.\*\$\/player\.\1\\\.handle=\\1\/\
		b\
	}\
/' > RealNames.sed
sed --regexp-extended --file=RealNames.sed Players.txt > x1
cat x1 | sed --regexp-extended '
	s/^player\.([^.]*)\.handle=(.*)$/	s\/\\\.\1\\\.\/\.\2\.\//
' > RealNames2.sed
cat >> RealNames2.sed <<EOF
	s/^([^.]*)\.([^.-]*)-([^.-]*)\.(team)=.*/\1\.\2-\3\.\4=\3/
	s/titles=\(\)/titles=/
EOF
cat New-Players.txt | sed --regexp-extended --file=RealNames2.sed > x2
cat > New-Players.txt <<EOF
#USCL Players

EOF
sort --ignore-case x2 | sed '
	/^$/ d
	s/^player\.\([^.]*\)\.name=/player.\1.handle=\1\n\0/
	/\.website=/ a\

' >> New-Players.txt
rm x1 x2 RealNames.sed RealNames2.sed
diff Players.txt New-Players.txt > Changes.txt
vim Changes.txt

