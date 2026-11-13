#!/usr/bin/env python3
"""Encode accessibility settings as bytes to append to the app's settings file (test devices only).

Usage: a11y_prefs.py 1=1 30=1 11=125
Each argument is <field number>=<integer value> from WholphinAccessibility.proto.
Appending a serialized message to a protobuf file merges it into what is already there.
Pass "reset" to clear every accessibility field back to its default first.
"""
import sys


def varint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


inner = b""
for arg in sys.argv[1:]:
    field, value = arg.split("=")
    inner += varint(int(field) << 3) + varint(int(value))
# Field 1000 of AppPreferences, length-delimited
sys.stdout.buffer.write(varint((1000 << 3) | 2) + varint(len(inner)) + inner)
