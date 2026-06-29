from abc import ABC, abstractmethod
from typing import Any, Dict, Optional


class BaseDecoder(ABC):
    """
    验证码解码器基类/接口
    所有新的验证码解码器（如 OCR、滑动组件、点选组件等）需继承此类
    """

    @property
    @abstractmethod
    def decoder_type(self) -> str:
        """解码器唯一标识类型，如 ddddocr, slider, text_select 等"""
        pass

    @abstractmethod
    def decode(self, img_bytes: bytes, extra_params: Optional[Dict[str, Any]] = None) -> Any:
        """
        核心解码/识别逻辑

        :param img_bytes: 图片二进制数据
        :param extra_params: 额外辅助参数（如提示词、坐标范围等）
        :return: 解码结果（如识别文本、缺口坐标dict等）
        """
        pass
