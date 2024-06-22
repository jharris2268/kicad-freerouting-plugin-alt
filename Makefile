freerouting_stdout.jar:
	cd freerouting_cli_stdout && make && cp freerouting_stdout.jar ../freerouting_alt/

freerouting_alt.zip: freerouting_stdout.jar
	cp plugin/freerouting_alt/*.py freerouting_alt/ && zip freerouting_alt.zip freerouting_alt/*
	
	
