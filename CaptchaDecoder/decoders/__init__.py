from typing import Dict, Optional
from .base import BaseDecoder
from .ddddocr_decoder import DdddOcrDecoder


class DecoderFactory:
    """
    验证码解码器工厂，单例管理并提供注册与获取功能
    """
    _registry: Dict[str, BaseDecoder] = {}

    @classmethod
    def register(cls, decoder: BaseDecoder):
        cls._registry[decoder.decoder_type.lower()] = decoder
        print(f"[DecoderFactory] Registered decoder type: '{decoder.decoder_type}'")

    @classmethod
    def get_decoder(cls, decoder_type: str = "ddddocr") -> Optional[BaseDecoder]:
        if not decoder_type:
            decoder_type = "ddddocr"
        decoder = cls._registry.get(decoder_type.lower())
        if not decoder and decoder_type.lower() == "default":
            return cls._registry.get("ddddocr")
        return decoder


# 自动注册系统内置解码器
DecoderFactory.register(DdddOcrDecoder())
