HOW TO BUILD THE MOD WITHOUT INSTALLING ANYTHING
===================================================
This uses GitHub Actions - a free cloud build service. Everything
compiles on GitHub's servers, which have internet access. You never
install Gradle, Java, or Fabric tools on your own computer.

STEP 1 - Create a free GitHub account
  Go to https://github.com/join if you don't already have one.

STEP 2 - Create a new repository
  Click the "+" in the top right -> "New repository".
  Name it "autofish-mod" (or anything). Keep it Public or Private,
  either works. Don't add a README/gitignore - leave it empty.

STEP 3 - Upload these files
  On the new repo's page, click "uploading an existing file" (or
  "Add file" -> "Upload files"). Drag in EVERYTHING from this folder,
  keeping the folder structure intact:
    - build.gradle
    - settings.gradle
    - gradle.properties
    - README.md
    - src/  (the whole folder, with all subfolders)
    - .github/  (the whole folder - this is the build instructions)

  GitHub's drag-and-drop upload preserves folder structure as long as
  you drag folders in, not just loose files. If it flattens things,
  use "git" via GitHub Desktop instead (also free, no command line).

STEP 4 - Commit
  Scroll down, click "Commit changes". This uploads the files and
  automatically triggers a build (because of the .github/workflows
  file).

STEP 5 - Watch the build run
  Click the "Actions" tab at the top of your repository. You'll see
  a build in progress (a yellow dot), then green check (success) or
  red X (failed) after a minute or two.

STEP 6 - Download the finished jar
  Click into the completed build run, scroll down to "Artifacts",
  and download "autofish-mod-jar". Unzip it - that's your real,
  compiled Fabric mod .jar file.

STEP 7 - Use it
  You still need Fabric Loader (https://fabricmc.net/use/) and
  Fabric API (https://modrinth.com/mod/fabric-api) installed for
  Minecraft 1.21.11. Drop autofish's jar, plus fabric-api's jar,
  into your .minecraft/mods folder.

IF THE BUILD FAILS (red X)
  Click into the failed run to see the error log. The most likely
  cause is that the exact version numbers in gradle.properties
  (yarn_mappings, loader_version, fabric_version) have moved on since
  I wrote them - check https://fabricmc.net/develop/ for current
  values, update gradle.properties in your repo, and it will
  automatically rebuild.
