.PHONY: build-CheckAurorasFunction

build-CheckAurorasFunction:
	# Build ClojureScript
	npm run build
	# Bundle with esbuild (tree shaking + minification)
	mkdir -p $(ARTIFACTS_DIR)/target
	npx esbuild target/main.js \
		--bundle \
		--platform=node \
		--target=node24 \
		--minify \
		--tree-shaking=true \
		--outfile=$(ARTIFACTS_DIR)/target/main.js
