#!/bin/bash
function resort () {
sed '
	/^$/ d
	/^#/ d
#	/\.handle=/ d
	/\.website=/ d
' | sort --ignore-case | sed '
	s/^player\.[^.]*\.//
'
}
resort < Players.txt > Sort.txt
resort < New-Players.txt > Sort-New.txt
diff Sort.txt Sort-New.txt | sed '
/^[0-9]/ {
	N
	N
	N
	/[0-9a-z]\+[\r\n]*< titles=[\r\n]*---[\r\n]*> titles=([A-Z]*)$/ d
}
' > Diff.txt
vi Diff.txt

