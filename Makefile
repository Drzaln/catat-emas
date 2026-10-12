.PHONY: build release install install-release ship clean tag

build:
	./gradlew assembleDebug

release:
	./gradlew assembleRelease

install: build
	adb install -r app/build/outputs/apk/debug/app-debug.apk

install-release: release
	adb install -r app/build/outputs/apk/release/app-release.apk

ship:
	./scripts/ship.sh $(BUMP)

clean:
	./gradlew clean

tag:
	@version=$$(grep -oE 'versionName = "[^"]+"' app/build.gradle.kts | tr -d '"' | sed 's/versionName = //'); \
	git tag -f "v$$version" && git push origin "v$$version" && echo "Tagged v$$version"
