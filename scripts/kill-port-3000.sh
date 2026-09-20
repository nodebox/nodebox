#!/bin/sh
lsof -ti :3000 | xargs kill