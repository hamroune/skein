#!/usr/bin/env bash
set -xe

packages="grpcio pyyaml cryptography pytest flake8 requests pyarrow nomkl"

if [[ $1 == "2.7" ]]; then
    conda create -n py27 python=2.7 $packages
    source activate py27
else
    conda install $packages
fi

pip install grpcio-tools

cd ~/skein
pip install -v --no-deps -e .

conda list
