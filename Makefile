
repl:
	lein repl

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

deploy:
	lein deploy clojars
