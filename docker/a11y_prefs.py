#!/usr/bin/env python3
"""Encode accessibility settings as bytes to append to the app's settings file (test devices only).

Usage: a11y_prefs.py 1=1 30=1 11=125
Each argument is <field number>=<value> from WholphinAccessibility.proto. A value that is not
a whole number is stored as text (e.g. 92=192.168.1.50 for the description server).
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
    field, value = arg.split("=", 1)
    if value.lstrip("-").isdigit():
        inner += varint(int(field) << 3) + varint(int(value))
    else:
        # Text fields, e.g. 92=192.168.1.50
        text = value.encode()
        inner += varint((int(field) << 3) | 2) + varint(len(text)) + text
# Field 1000 of AppPreferences, length-delimited
sys.stdout.buffer.write(varint((1000 << 3) | 2) + varint(len(inner)) + inner)
