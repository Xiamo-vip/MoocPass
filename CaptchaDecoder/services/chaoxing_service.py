from random import randint
from typing import Optional, Tuple
import requests
import urllib3
from decoders import DecoderFactory

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)



class ChaoxingCaptchaService:
    host = 'https://mooc1.chaoxing.com'
    api = {
        'get': '/processVerifyPng.ac',
        'submit': '/html/processVerify.ac'
    }

    @classmethod
    def try_pass(cls, user_agent: str, cookies: str, decoder_type: str = "ddddocr") -> Tuple[bool, str]:
        session = requests.session()
        session.headers.update({
            'User-Agent': user_agent,
            'Cookie': cookies,
            'Accept': 'image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8'
        })
        session.verify = False

        # 1. 获取验证码图片
        get_api = cls.host + cls.api['get']
        random_t = randint(0, 2147483647)
        try:
            res = session.get(get_api, params={'t': random_t}, timeout=10)
            if res.status_code != 200 or 'image' not in res.headers.get('Content-Type', ''):
                return False, "Fetch captcha image failed"
            img_bytes = res.content
        except Exception as e:
            return False, f"Network error fetching captcha: {e}"

        # 2. 获取匹配的解码器并解码
        decoder = DecoderFactory.get_decoder(decoder_type)
        if not decoder:
            return False, f"Decoder type '{decoder_type}' not found"

        try:
            code = decoder.decode(img_bytes)
        except Exception as e:
            return False, f"Decoding error: {e}"

        # 3. 提交验证码
        submit_api = cls.host + cls.api['submit']
        params = {'ucode': code, 'app': 0}
        try:
            sub_res = session.get(submit_api, params=params, allow_redirects=False, timeout=10)
            if sub_res.status_code in (302, 200):
                return True, str(code)
            return False, f"Submit returned status code {sub_res.status_code}"
        except Exception as e:
            return False, f"Network error submitting captcha: {e}"
