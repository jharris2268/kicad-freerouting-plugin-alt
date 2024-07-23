.PHONY: default
default: freerouting_alt.zip

freerouting_stdout.jar:
	cd freerouting_cli_stdout && make && cp build/obj/freerouting_stdout.jar  ../freerouting_alt/plugins/

freerouting_alt.zip: freerouting_stdout.jar
	cp plugin/freerouting_alt/*.py plugin/gui/dialog_base.py freerouting_cli_stdout/build/obj/freerouting_stdout.jar resources/* freerouting_alt/plugins/ \
		&& cp plugin/freerouting_alt/dsn/*.py freerouting_alt/plugins/dsn/ \
		&& cd freerouting_alt \
		&& zip ../freerouting_alt.zip `find | grep -v __pycache__ | grep -v git`
	
	
clean:
	rm freerouting_alt.zip freerouting_alt/plugins/*.py freerouting_alt/plugins/*.jar
	
