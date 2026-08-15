# Internal one-shot packaging. CI does not use this file.
#
#   make package   natives this host can build + fat/classifier JARs
#   make publish   package, then deploy using pom.xml <distributionManagement>
#
# Version is the git tag on HEAD with a leading v stripped (v0.1.8 → 0.1.8).
# Override:            make publish VERSION=0.1.9
# Override platforms:  make package CLASSIFIERS='linux-x86_64 linux-aarch64'
# Extra Maven flags:   make publish MVN_FLAGS='-B -DskipNative -DskipTests --settings /path/to/settings.xml'
#
# Server credentials come from ~/.m2/settings.xml; the <server><id> must match
# the repository id in pom.xml. Do not put private repo URLs in this tree.

.DEFAULT_GOAL := help

MVN         ?= mvn
MVN_FLAGS   ?= -B -DskipNative -DskipTests
CLASSIFIERS ?= --all
ARTIFACT_ID ?= anydoc
VERSION     ?= $(shell sh scripts/git-version.sh 2>/dev/null)

.PHONY: help native package jars publish deploy test clean require-version

help:
	@echo "Targets:"
	@echo "  make native    build JNI libs ($(CLASSIFIERS))"
	@echo "  make package   native + fat JAR + per-platform JARs"
	@echo "  make publish   package and deploy via pom.xml distributionManagement"
	@echo "  make test      native + unit tests"
	@echo "  make clean     Maven + staged natives"
	@echo
	@echo "Version: $(if $(VERSION),$(VERSION),<not on a git tag>)  (override VERSION=...)"
	@echo "Variables: VERSION CLASSIFIERS MVN MVN_FLAGS"

require-version:
	@test -n "$(VERSION)" || { echo "error: HEAD is not a git tag; checkout a vX.Y.Z tag or set VERSION=" >&2; exit 1; }

native:
	sh scripts/build-native.sh $(CLASSIFIERS)

jars: require-version
	$(MVN) $(MVN_FLAGS) -Drevision=$(VERSION) package
	VERSION=$(VERSION) sh scripts/package-jars.sh

package: native jars

test:
	$(MVN) -B test

# Deploy the fat artifact (and POM) first, then each classifier JAR to the
# same repository the POM names. Repo id/url are read from pom.xml so an
# internal branch can retarget the private server without changing this file.
publish: require-version package
	$(MVN) $(MVN_FLAGS) -Drevision=$(VERSION) deploy
	@set -eu; \
	version=$(VERSION); \
	group=$$(sed -n 's/^groupId=//p' target/maven-coordinates.properties); \
	artifact=$$(sed -n 's/^artifactId=//p' target/maven-coordinates.properties); \
	eval "$$(awk -v ver="$$version" ' \
	  /<distributionManagement>/ { dm=1 } \
	  dm && /<\/distributionManagement>/ { dm=0 } \
	  dm && /<snapshotRepository>/ { sr=1 } \
	  dm && /<\/snapshotRepository>/ { sr=0 } \
	  dm && !sr && /<repository>/ { r=1 } \
	  dm && r && /<\/repository>/ { r=0 } \
	  dm && r && /<id>/  { sub(/.*<id>/,"");  sub(/<\/id>.*/,"");  rid=$$0 } \
	  dm && r && /<url>/ { sub(/.*<url>/,""); sub(/<\/url>.*/,""); rurl=$$0 } \
	  dm && sr && /<id>/  { sub(/.*<id>/,"");  sub(/<\/id>.*/,"");  sid=$$0 } \
	  dm && sr && /<url>/ { sub(/.*<url>/,""); sub(/<\/url>.*/,""); surl=$$0 } \
	  END { \
	    if (ver ~ /SNAPSHOT/ && sid != "") { printf "repo_id=%s\nrepo_url=%s\n", sid, surl } \
	    else { printf "repo_id=%s\nrepo_url=%s\n", rid, rurl } \
	  }' pom.xml)"; \
	if [ -z "$$repo_id" ] || [ -z "$$repo_url" ]; then \
	  echo "error: pom.xml is missing <distributionManagement> repository id/url" >&2; \
	  exit 1; \
	fi; \
	echo "deploying classifiers to $$repo_id"; \
	for jar in target/$${artifact}-$${version}-*.jar; do \
	  [ -f "$$jar" ] || continue; \
	  classifier=$${jar#target/$${artifact}-$${version}-}; \
	  classifier=$${classifier%.jar}; \
	  echo "  $$classifier"; \
	  $(MVN) -B org.apache.maven.plugins:maven-deploy-plugin:3.1.3:deploy-file \
	    $(MVN_FLAGS) \
	    -DgroupId="$$group" \
	    -DartifactId="$$artifact" \
	    -Dversion="$$version" \
	    -Dpackaging=jar \
	    -Dclassifier="$$classifier" \
	    -Dfile="$$jar" \
	    -DgeneratePom=false \
	    -DrepositoryId="$$repo_id" \
	    -Durl="$$repo_url"; \
	done

deploy: publish

clean:
	$(MVN) -B -DskipNative clean
	rm -rf native/dist
