{
  description = "A devShell example";

  inputs = {
    nixpkgs.url      = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url  = "github:numtide/flake-utils";
  };

  outputs = { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;

          config.allowUnfree = true;
        };
      in
      {
        devShells.default = with pkgs; mkShell {
          buildInputs = [
            maven
            jdk21

            chromedriver

            jdt-language-server
          ];

          GOOGLE_CHROME_BINARY="${google-chrome}/bin/google-chrome-stable";

          shellHook = ''
            alias m=./run.sh
          '';
        };
      }
    );
}
