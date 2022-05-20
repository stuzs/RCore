#!/bin/sh
export CHISEL_PROJ=`pwd`
alias cdt="cd $CHISEL_PROJ"
pushd .
cd ../..
export CHISEL_WORKSPACE_PATH=`pwd`
popd
