
repl:
	lein repl

.PHONY: test
test:
	lein test

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

release:
	lein release
