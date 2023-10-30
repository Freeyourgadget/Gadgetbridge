# File downloaded from https://github.com/dakhnod/Fossil-HR-SDK/

import sys
import os
import json
import crc32c
import getopt

class Packer:
    def __init__(self):
        self.file_block = bytearray()

    def put_int(self, content, length=4):
        self.file_block.extend(content.to_bytes(length, 'little'))

    def pack(self, input_dir_path, output_file_path):
        start_path = os.getcwd()

        if not os.path.isdir(input_dir_path):
            print('cannot find dir %s' % input_dir_path)
            exit()
        os.chdir(input_dir_path)

        with open('app.json', 'r') as json_file:
            app_meta = json.load(json_file)

        os.chdir('files')

        all_files = []
        dir_sizes = {}

        for files_dir_list in [('code', False), ('icons', False), ('layout', True), ('display_name', True), ('config', True)]:
            dir_size = 0
            files_dir = files_dir_list[0]
            append_null = files_dir_list[1]
            files = os.listdir(files_dir)
            os.chdir(files_dir)
            for file in sorted(files):
                print(f'packing {file}')
                with open(file, 'rb')as f:
                    contents = bytearray(f.read())
                    if append_null:
                        contents.append(0)
                    file_size = contents.__len__()
                    all_files.append({
                        'filename': file,
                        'contents': contents,
                        'size': file_size
                    })
                    dir_size = dir_size + file_size + file.__len__() + 4 # null byte + size bytes
            os.chdir(os.pardir)
            dir_sizes[files_dir] = dir_size

        offset_code = 88
        offset_icons = offset_code + dir_sizes['code']
        offset_layout = offset_icons + dir_sizes['icons']
        offset_display_name = offset_layout + dir_sizes['layout']
        offset_config = offset_display_name + dir_sizes['display_name']
        offset_file_end = offset_config + dir_sizes['config']

        self.file_block.extend([int(octet) for octet in app_meta['version'].split('.')])

        self.put_int(0)
        self.put_int(0)
        self.put_int(offset_code)
        self.put_int(offset_icons)
        self.put_int(offset_layout)
        self.put_int(offset_display_name)
        self.put_int(offset_display_name)
        self.put_int(offset_config)
        self.put_int(offset_file_end)
        self.put_int(0)
        self.put_int(0)
        self.put_int(0)
        self.put_int(0)
        self.put_int(0)
        self.put_int(0)
        self.put_int(0)
        self.put_int(0)
        self.put_int(0)

        for file in all_files:
            filename = file['filename']
            self.put_int(filename.__len__() + 1, 1)
            self.file_block.extend(filename.encode('utf-8'))
            self.put_int(0, 1) # null byte ending
            self.put_int(file['size'], 2)
            self.file_block.extend(file['contents'])

        os.chdir(start_path)

        identifier = all_files[0]['filename']

        full_file = bytearray()
        full_file.extend([0xFE, 0x15]) # file handle
        full_file.extend([0x03, 0x00]) # file version
        full_file.extend(int(0).to_bytes(4, 'little')) # file offset
        full_file.extend(self.file_block.__len__().to_bytes(4, 'little')) # file size
        full_file.extend(self.file_block)
        full_file.extend(crc32c.crc32c(self.file_block).to_bytes(4, 'little'))

        if output_file_path is None:
            output_file_path = identifier

        with open(output_file_path, 'wb') as output_file:
            output_file.write(full_file)



def main():
    packer = Packer()
    input_dir_path = None
    output_file_path = None
    args, remainder = getopt.getopt(sys.argv[1:], 'i:o:', ['input=', 'output='])
    for key, value in args:
        if key in ['-i', '--input']:
            input_dir_path = value
        elif key in ['-o', '--output']:
            output_file_path = value
    packer.pack(input_dir_path, output_file_path)


if __name__ == '__main__':
    main()
