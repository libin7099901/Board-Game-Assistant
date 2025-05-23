#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Stop when a command exits with an error
set -e

# Determine the directory of this script
SCRIPT_DIR_PATH="$(cd "$(dirname "$0")" && pwd -P)"

# The directory that contains the gradle wrapper
WRAPPER_DIR_PATH="$SCRIPT_DIR_PATH/gradle/wrapper"

# The path to the gradle wrapper jar
WRAPPER_JAR_PATH="$WRAPPER_DIR_PATH/gradle-wrapper.jar"

# The path to the gradle wrapper properties
WRAPPER_PROPS_PATH="$WRAPPER_DIR_PATH/gradle-wrapper.properties"

# The version of gradle to use
# Retrieved from the wrapper properties file
GRADLE_VERSION=

# The url to download gradle from
# Retrieved from the wrapper properties file
GRADLE_DOWNLOAD_URL=

# The SHA256 checksum of the gradle distribution
# Retrieved from the wrapper properties file
GRADLE_DOWNLOAD_SHA256_SUM=

# The directory to install gradle to
GRADLE_INSTALL_DIR=

# The name of the gradle distribution zip file
GRADLE_DISTRIBUTION_ZIP_NAME=

# The path to the gradle distribution zip file
GRADLE_DISTRIBUTION_ZIP_PATH=

# The directory to unpack the gradle distribution to
GRADLE_DISTRIBUTION_UNPACK_DIR=

# Set the GRADLE_USER_HOME if not already set
if [ -z "$GRADLE_USER_HOME" ]; then
    GRADLE_USER_HOME_DEFAULT_DIR_PATH="$HOME/.gradle"
    if [ -d "$GRADLE_USER_HOME_DEFAULT_DIR_PATH" ]; then
        GRADLE_USER_HOME="$GRADLE_USER_HOME_DEFAULT_DIR_PATH"
        export GRADLE_USER_HOME
    else
        # This is not critical, so we don't need to stop
        echo "GRADLE_USER_HOME is not set, and $GRADLE_USER_HOME_DEFAULT_DIR_PATH does not exist."
    fi
fi

#
# Helper functions
#
# Output an error message and exit
#
# Args:
#   $1: The error message
error() {
    echo "ERROR: $1" >&2
    exit 1
}

#
# Output a warning message
#
# Args:
#   $1: The warning message
warning() {
    echo "WARNING: $1" >&2
}

#
# Output an info message
#
# Args:
#   $1: The info message
info() {
    echo "INFO: $1"
}

#
# Check if a command exists
#
# Args:
#   $1: The command to check
#
# Returns:
#   0 if the command exists, 1 otherwise
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

#
# Download a file from a url
#
# Args:
#   $1: The url to download from
#   $2: The path to save the file to
download() {
    local url="$1"
    local path="$2"

    info "Downloading $url to $path"

    if command_exists curl; then
        curl --fail --location --output "$path" "$url"
    elif command_exists wget; then
        wget --quiet --output-document="$path" "$url"
    else
        error "Cannot download gradle distribution. Neither curl nor wget is available."
    fi
}

#
# Check the SHA256 checksum of a file
#
# Args:
#   $1: The path to the file
#   $2: The expected SHA256 checksum
check_sha256_checksum() {
    local file_path="$1"
    local expected_checksum="$2"

    info "Checking SHA256 checksum of $file_path"

    local actual_checksum=
    if command_exists sha256sum; then
        actual_checksum="$(sha256sum "$file_path" | awk '{print $1}')"
    elif command_exists shasum; then
        actual_checksum="$(shasum -a 256 "$file_path" | awk '{print $1}')"
    else
        warning "Cannot check SHA256 checksum. Neither sha256sum nor shasum is available."
        return
    fi

    if [ "$actual_checksum" != "$expected_checksum" ]; then
        error "SHA256 checksum mismatch for $file_path. Expected $expected_checksum, but got $actual_checksum"
    fi
}

#
# Unpack a zip file
#
# Args:
#   $1: The path to the zip file
#   $2: The directory to unpack to
unpack_zip() {
    local zip_file_path="$1"
    local unpack_dir_path="$2"

    info "Unpacking $zip_file_path to $unpack_dir_path"

    if command_exists unzip; then
        unzip -q -d "$unpack_dir_path" "$zip_file_path"
    else
        error "Cannot unpack gradle distribution. unzip is not available."
    fi
}

#
# Read the gradle wrapper properties
#
read_wrapper_properties() {
    info "Reading wrapper properties from $WRAPPER_PROPS_PATH"
    if [ ! -f "$WRAPPER_PROPS_PATH" ]; then
        error "Gradle wrapper properties file not found: $WRAPPER_PROPS_PATH"
    fi

    # Read properties file line by line
    # and extract key-value pairs
    # This is a simple parser and may not work for all cases
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip comments and empty lines
        if echo "$line" | grep -q -e '^#' -e '^[[:space:]]*$'; then
            continue
        fi

        # Extract key and value
        # This assumes that the key and value are separated by '='
        # and that there are no spaces around the '='
        local key="$(echo "$line" | cut -d'=' -f1)"
        local value="$(echo "$line" | cut -d'=' -f2-)"

        # Assign values to variables based on the key
        case "$key" in
            distributionUrl)
                GRADLE_DOWNLOAD_URL="$value"
                ;;
            distributionSha256Sum)
                GRADLE_DOWNLOAD_SHA256_SUM="$value"
                ;;
            *)
                # Ignore unknown properties
                ;;
        esac
    done < "$WRAPPER_PROPS_PATH"

    if [ -z "$GRADLE_DOWNLOAD_URL" ]; then
        error "Could not determine gradle distribution URL from $WRAPPER_PROPS_PATH"
    fi
}

#
# Determine gradle version from download url
# (e.g. https://services.gradle.org/distributions/gradle-7.4-all.zip -> 7.4)
#
determine_gradle_version() {
    GRADLE_VERSION="$(echo "$GRADLE_DOWNLOAD_URL" | sed -n 's/.*gradle-\([0-9.]*\)-.*/\1/p')"
    if [ -z "$GRADLE_VERSION" ]; then
        error "Could not determine gradle version from download URL: $GRADLE_DOWNLOAD_URL"
    fi
    info "Gradle version: $GRADLE_VERSION"
}

#
# Prepare gradle installation
#
prepare_gradle_installation() {
    # The directory where gradle distributions are stored
    local gradle_user_home_dists_dir_path="$GRADLE_USER_HOME/wrapper/dists"

    # The name of the gradle distribution (e.g. gradle-7.4-all)
    GRADLE_DISTRIBUTION_ZIP_NAME="$(basename "$GRADLE_DOWNLOAD_URL" .zip)"

    # The directory for this specific gradle distribution
    # (e.g. $GRADLE_USER_HOME/wrapper/dists/gradle-7.4-all)
    local gradle_distribution_dir_path="$gradle_user_home_dists_dir_path/$GRADLE_DISTRIBUTION_ZIP_NAME"

    # The path to the downloaded gradle distribution zip file
    # (e.g. $GRADLE_USER_HOME/wrapper/dists/gradle-7.4-all/abcdef123456/gradle-7.4-all.zip)
    # The random string is part of how gradle stores distributions
    # We need to find the correct directory if it already exists
    local random_string_dir_name=""
    if [ -d "$gradle_distribution_dir_path" ]; then
        # Find the first directory inside the distribution directory
        # This assumes there is only one such directory, or the first one is correct
        # shellcheck disable=SC2012 # ls output used safely
        random_string_dir_name=$(ls "$gradle_distribution_dir_path" | head -n 1)
    fi

    if [ -z "$random_string_dir_name" ]; then
        # If the directory doesn't exist, create a new one with a placeholder name
        # Gradle itself will create a directory with a hash-based name later if it downloads
        random_string_dir_name="temp-download-dir"
    fi
    
    local gradle_distribution_specific_dir_path="$gradle_distribution_dir_path/$random_string_dir_name"
    GRADLE_DISTRIBUTION_ZIP_PATH="$gradle_distribution_specific_dir_path/$GRADLE_DISTRIBUTION_ZIP_NAME.zip"
    GRADLE_DISTRIBUTION_UNPACK_DIR="$gradle_distribution_specific_dir_path" # Unpack directly into this folder

    GRADLE_INSTALL_DIR="$GRADLE_DISTRIBUTION_UNPACK_DIR/gradle-$GRADLE_VERSION"

    # Download gradle if not already downloaded
    if [ ! -f "$GRADLE_DISTRIBUTION_ZIP_PATH" ]; then
        # Ensure the target directory exists
        mkdir -p "$gradle_distribution_specific_dir_path"
        download "$GRADLE_DOWNLOAD_URL" "$GRADLE_DISTRIBUTION_ZIP_PATH"
        if [ -n "$GRADLE_DOWNLOAD_SHA256_SUM" ]; then
            check_sha256_checksum "$GRADLE_DISTRIBUTION_ZIP_PATH" "$GRADLE_DOWNLOAD_SHA256_SUM"
        fi
    fi

    # Unpack gradle if not already unpacked
    if [ ! -d "$GRADLE_INSTALL_DIR" ]; then
        unpack_zip "$GRADLE_DISTRIBUTION_ZIP_PATH" "$GRADLE_DISTRIBUTION_UNPACK_DIR"
        # Check if the unpacking was successful
        if [ ! -d "$GRADLE_INSTALL_DIR" ]; then
            error "Failed to unpack Gradle distribution to $GRADLE_INSTALL_DIR"
        fi
    fi
}


# Main script execution starts here

# Read properties like distributionUrl
read_wrapper_properties

# Determine Gradle version from the URL
determine_gradle_version

# Prepare Gradle installation (download and unpack if necessary)
prepare_gradle_installation

# Construct the command to run gradle
GRADLE_OPTS_ESCAPED=$(echo "$JAVA_OPTS $GRADLE_OPTS" | sed 's/ /\\ /g')
GRADLE_CMD="$GRADLE_INSTALL_DIR/bin/gradle"

# Execute gradle
info "Executing Gradle: $GRADLE_CMD $GRADLE_OPTS_ESCAPED \"$@\""
# shellcheck disable=SC2206 # Word splitting is intended here for arguments
exec "$GRADLE_CMD" $JAVA_OPTS $GRADLE_OPTS "$@"
