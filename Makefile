.PHONY: default
default: freerouting_alt.zip

freerouting_stdout.jar:
	cd freerouting_cli_stdout && make && cp build/obj/freerouting_stdout.jar ../freerouting_alt/plugins/

freerouting_alt.zip: freerouting_stdout.jar
	cp plugin/freerouting_alt/*.py resources/* freerouting_alt/plugins/ && cd freerouting_alt && zip ../freerouting_alt.zip * plugins/*
	
	
clean:
	rm freerouting_alt.zip freerouting_alt/plugins/*.py freerouting_alt/plugins/*.jar
	
