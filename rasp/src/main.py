from config.config import loadJson

def main():
    print("Starting rasp pi...")
    config = loadJson()

if __name__ == "__main__":
    main()