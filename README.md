# dots

A small command-line tool for managing dotfiles with symlinks, built with Kotlin/Native for macOS (arm64).

Inspired by [bonclay](https://github.com/talal/bonclay). Unlike bonclay, the config always lives at `~/.config/dots/config`, so you never pass it on the command line.

## Install

Can be easily installed with [Homebrew](https://brew.sh):

```sh
brew tap nrlquaker/personal
brew install dots
```

Alternatively, binaries are available in the [GitHub Releases](https://github.com/nrlquaker/dots/releases)

## Config

`~/.config/dots/config` is a JSON file. Each entry under `dots` maps a `source` (where the file lives on your system) to a `target` (where it is stored in your dotfiles repo). Paths must be absolute or start with `~`.

```json
{
  "backup":  { "overwrite": false },
  "restore": { "overwrite": false },
  "sync":    { "overwrite": false },
  "dots": {
    "config": { "source": "~/.config/dots/config", "target": "~/dotfiles/dots/config" },
    "zsh":    { "source": "~/.zshrc",              "target": "~/dotfiles/zshrc" }
  }
}
```

The option blocks are optional. A flag on the command line always beats the config, for example `--no-overwrite` wins over `"overwrite": true`.

## Commands

| Command | What it does |
|---|---|
| `dots init` | Create a template config. `dots init <path>` links an existing config instead. |
| `dots backup` | Move each source to its target and leave a symlink in its place. |
| `dots restore` | Undo backup: remove the symlink and move the file back. |
| `dots sync` | Create missing symlinks for targets that already exist. Nothing is moved. |
| `dots clean` | Remove broken symlinks that dots manages. |
| `dots list` | Show every entry and whether it is linked (✓) or not (✗). |

`backup`, `restore` and `sync` take `-o/--overwrite` to replace whatever is in the way. Every entry is checked first, and if any check fails nothing is changed.

## New machine

Keep the config in your dotfiles repo (the `config` entry above does that), then:

```sh
git clone <your-dotfiles> ~/dotfiles
dots init ~/dotfiles/dots/config   # links ~/.config/dots/config to the repo copy
dots sync                          # links everything else
```

## Development

```sh
./gradlew nativeTest       # run tests
./gradlew packageRelease   # build/dist/dots-<version>-macos-arm64.zip
```

The version is set in `gradle.properties`.
