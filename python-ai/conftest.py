"""让 pytest 能找到 app 包（不依赖安装）。"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
