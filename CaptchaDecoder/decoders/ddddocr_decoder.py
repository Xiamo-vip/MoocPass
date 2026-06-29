from typing import Any, Dict, Optional
import ddddocr
from .base import BaseDecoder


class DdddOcrDecoder(BaseDecoder):
    """
    基于 DdddOcr 的通用图片验证码识别器
    """

    def __init__(self):
        print("[DecoderInit] Initializing DdddOcr instance...")
        self._ocr = ddddocr.DdddOcr(show_ad=False)
        print("[DecoderInit] DdddOcr initialized successfully!")

    @property
    def decoder_type(self) -> str:
        return "ddddocr"

    def decode(self, img_bytes: bytes, extra_params: Optional[Dict[str, Any]] = None) -> str:
        if not img_bytes:
            raise ValueError("Empty image bytes provided to DdddOcrDecoder")
        
        result = self._ocr.classification(img_bytes)
        return result
