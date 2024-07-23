#!/bin/sh

gen()
{
	sed "
		s/<<ver>>/$ver/g
		s/<<date>>/$date/g
		s/<<time>>/$time/g
		s/<<user>>/$user/g
		s/<<javaver>>/$javaver/g
	"
}

# generate build-time sources (versions, build date, etc)
ver="1.4.5-CLI-$rev"
date=`date +%Y-%m-%d`
time=`date +%H:%M:%S%z`
user=`whoami`
javaver=`javac -version | sed "s/javac //"`

gen < Constants.java.in > Constants.java
gen < MANIFEST.MF.in > MANIFEST.MF

