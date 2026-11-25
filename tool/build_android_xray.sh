#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 1 ]]; then
  echo "Usage: $0 /path/to/libv2ray.aar" >&2
  exit 2
fi

tool_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
output=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")
sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
: "${sdk_root:?Set ANDROID_SDK_ROOT or ANDROID_HOME}"

wrapper_version=26.11.9
wrapper_sha256=e1ab0bc1514aa7624c8d1fdbdff26b65d818cea3b3d412ca17999845e3a2ed2d
mobile_version=v0.0.0-20261011034528-197770444867
ndk_version=29.0.14206865
android_api=21
page_size=16384

export GOTOOLCHAIN=go1.27.3
export ANDROID_HOME=$sdk_root
export ANDROID_SDK_ROOT=$sdk_root
export ANDROID_NDK_HOME=$sdk_root/ndk/$ndk_version
if [[ ! -d $ANDROID_NDK_HOME ]]; then
  echo "Install Android NDK $ndk_version under $sdk_root/ndk" >&2
  exit 1
fi

scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT
archive=$scratch/AndroidLibXrayLite.tar.gz
curl --fail --silent --show-error --location \
  "https://codeload.github.com/2dust/AndroidLibXrayLite/tar.gz/refs/tags/v$wrapper_version" \
  --output "$archive"
printf '%s  %s\n' "$wrapper_sha256" "$archive" | shasum -a 256 --check -
tar -xzf "$archive" -C "$scratch"

export GOBIN=$scratch/bin
export PATH=$GOBIN:$PATH
go install "golang.org/x/mobile/cmd/gomobile@$mobile_version"
go install "golang.org/x/mobile/cmd/gobind@$mobile_version"

cd "$scratch/AndroidLibXrayLite-$wrapper_version"
cp "$tool_dir"/android_xray/*.go .
gomobile bind -target=android -androidapi "$android_api" -trimpath \
  -ldflags="-s -w -buildid= -checklinkname=0 -linkmode=external -extldflags=-Wl,-z,max-page-size=$page_size,-z,common-page-size=$page_size" \
  -o "$scratch/libv2ray.aar" ./

install -m 0644 "$scratch/libv2ray.aar" "$output"
shasum -a 256 "$output"
